package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AtletaService {

    @Autowired
    private AtletaRepository atletaRepository;

    @Autowired
    private FirestoreDirectService directService;

    @Autowired
    private CloudStorageService cloudStorageService;

    // --- MÉTODOS DE LEITURA E DELEÇÃO (permanecem os mesmos) ---
    public Flux<Atleta> findAll() {
        return atletaRepository.findAll();
    }

    public Mono<Atleta> findById(String id) {
        return atletaRepository.findById(id);
    }

    public Mono<Void> deleteById(String id) {
        // Futuramente, adicionar lógica para deletar as fotos do Cloud Storage
        return atletaRepository.deleteById(id);
    }

    // --- NOVOS MÉTODOS DE ESCRITA COM LÓGICA DE GALERIA ---

    /**
     * Cria uma nova atleta com uma galeria de fotos.
     * @param dto DTO com os dados da atleta e metadados das fotos.
     * @param files Fluxo de arquivos a serem enviados.
     * @return Mono<Atleta> A atleta salva.
     */
    public Mono<Atleta> createAtletaWithGallery(AtletaFormDTO dto, Flux<FilePart> files) {
        // 1. Agrupa os arquivos recebidos em um mapa (filename -> FilePart) para fácil acesso.
        Mono<Map<String, FilePart>> filesMapMono = files
                .collectMap(FilePart::filename)
                .defaultIfEmpty(Map.of());

        return filesMapMono.flatMap(filesMap -> {
            // 2. Processa cada FotoDTO da requisição.
            Flux<FotoAcervo> fotosProcessadasFlux = Flux.fromIterable(dto.fotos())
                    .flatMap(fotoDto -> {
                        FilePart file = filesMap.get(fotoDto.filename());
                        if (file != null) {
                            // É uma foto nova, precisa fazer upload.
                            return cloudStorageService.uploadFile(file)
                                    .map(url -> new FotoAcervo(url, fotoDto.legenda()));
                        } else {
                            // É uma foto existente (improvável na criação, mas bom ter).
                            return Mono.empty();
                        }
                    });

            // 3. Coleta todas as fotos processadas em uma lista.
            return fotosProcessadasFlux.collectList().flatMap(fotosDaGaleria -> {
                // 4. Monta o objeto Atleta final.
                Atleta novaAtleta = new Atleta();
                novaAtleta.setNome(dto.nome());
                novaAtleta.setModalidade(dto.modalidade());
                novaAtleta.setBiografia(dto.biografia());
                novaAtleta.setCompeticao(dto.competicao());
                novaAtleta.setFotos(fotosDaGaleria);

                // 5. Define a foto destaque.
                definirFotoDestaque(novaAtleta, dto.fotos());

                // 6. Salva a atleta usando nosso serviço "Álcool" (direto no Firestore).
                return directService.saveAtleta(novaAtleta);
            });
        });
    }

    /**
     * Atualiza uma atleta existente e sua galeria de fotos.
     * @param id O ID da atleta a ser atualizada.
     * @param dto DTO com os novos dados.
     * @param files Fluxo de novos arquivos a serem enviados.
     * @return Mono<Atleta> A atleta atualizada.
     */
    public Mono<Atleta> updateAtletaWithGallery(String id, AtletaFormDTO dto, Flux<FilePart> files) {
        // 1. Busca a atleta existente no banco.
        Mono<Atleta> atletaExistenteMono = atletaRepository.findById(id);

        // 2. Agrupa os novos arquivos em um mapa.
        Mono<Map<String, FilePart>> filesMapMono = files
                .collectMap(FilePart::filename)
                .defaultIfEmpty(Map.of());

        // 3. Combina as duas informações (atleta e mapa de arquivos).
        return Mono.zip(atletaExistenteMono, filesMapMono)
                .flatMap(tuple -> {
                    Atleta atletaExistente = tuple.getT1();
                    Map<String, FilePart> filesMap = tuple.getT2();

                    // 4. Processa cada FotoDTO da requisição para construir a nova galeria.
                    Flux<FotoAcervo> fotosProcessadasFlux = Flux.fromIterable(dto.fotos())
                            .flatMap(fotoDto -> {
                                FilePart file = filesMap.get(fotoDto.filename());
                                if (file != null) {
                                    // É uma foto nova, faz o upload.
                                    return cloudStorageService.uploadFile(file)
                                            .map(url -> new FotoAcervo(url, fotoDto.legenda()));
                                } else if (fotoDto.url() != null && !fotoDto.url().isBlank()) {
                                    // É uma foto existente que foi mantida. Apenas recria o objeto.
                                    FotoAcervo fotoExistente = new FotoAcervo(fotoDto.url(), fotoDto.legenda());
                                    fotoExistente.setId(fotoDto.id()); // Preserva o ID original.
                                    return Mono.just(fotoExistente);
                                } else {
                                    // FotoDTO sem arquivo ou URL, ignora.
                                    return Mono.empty();
                                }
                            });

                    // 5. Coleta a nova lista de fotos e atualiza a atleta.
                    return fotosProcessadasFlux.collectList().flatMap(novaListaDeFotos -> {
                        // Atualiza os campos básicos.
                        atletaExistente.setNome(dto.nome());
                        atletaExistente.setModalidade(dto.modalidade());
                        atletaExistente.setBiografia(dto.biografia());
                        atletaExistente.setCompeticao(dto.competicao());
                        atletaExistente.setFotos(novaListaDeFotos);

                        // 6. Redefine a foto destaque.
                        definirFotoDestaque(atletaExistente, dto.fotos());

                        // 7. Salva a atleta atualizada usando o serviço "Álcool".
                        return directService.saveAtleta(atletaExistente);
                    });
                });
    }

    /**
     * Método auxiliar para encontrar e definir a foto destaque na atleta.
     * @param atleta A entidade Atleta a ser modificada.
     * @param fotosDto A lista de DTOs vinda do frontend.
     */
    private void definirFotoDestaque(Atleta atleta, List<FotoDTO> fotosDto) {
        // Encontra o DTO da foto que foi marcada como destaque no frontend.
        FotoDTO destaqueDto = fotosDto.stream()
                .filter(FotoDTO::ehDestaque)
                .findFirst()
                .orElse(fotosDto.isEmpty() ? null : fotosDto.get(0)); // Fallback para a primeira foto.

        if (destaqueDto != null && atleta.getFotos() != null) {
            // Encontra a FotoAcervo correspondente na lista final da atleta.
            // A correspondência é feita pela legenda, pois o ID/URL pode ser novo.
            // Uma abordagem mais robusta poderia usar o 'filename' ou um ID temporário.
            atleta.getFotos().stream()
                    .filter(f -> Objects.equals(f.getLegenda(), destaqueDto.legenda()))
                    .findFirst()
                    .ifPresent(fotoDeDestaque -> {
                        // Marca a foto correta como destaque e define o ID principal na atleta.
                        atleta.getFotos().forEach(f -> f.setEhDestaque(false)); // Reseta todas.
                        fotoDeDestaque.setEhDestaque(true);
                        atleta.setFotoDestaqueId(fotoDeDestaque.getId());
                    });
        } else {
            atleta.setFotoDestaqueId(null);
        }
    }
}