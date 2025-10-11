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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AtletaService {

    @Autowired
    private AtletaRepository atletaRepository;
    @Autowired
    private FirestoreDirectService directService;
    @Autowired
    private CloudStorageService cloudStorageService;

    // --- MÉTODOS DE LEITURA E DELEÇÃO (sem alterações) ---
    public Flux<Atleta> findAll() { return atletaRepository.findAll(); }
    public Mono<Atleta> findById(String id) { return atletaRepository.findById(id); }
    public Mono<Void> deleteById(String id) { return atletaRepository.deleteById(id); }

    // --- MÉTODOS DE ESCRITA ATUALIZADOS ---

    public Mono<Atleta> createAtletaWithGallery(AtletaFormDTO dto, Flux<FilePart> files) {
        return files.collectMap(FilePart::filename).flatMap(filesMap ->
                Flux.fromIterable(dto.fotos())
                        .flatMap(fotoDto -> {
                            FilePart file = filesMap.get(fotoDto.filename());
                            return file != null ? cloudStorageService.uploadFile(file)
                                    .map(url -> new FotoAcervo(url, fotoDto.legenda())) : Mono.empty();
                        })
                        .collectList().flatMap(fotosDaGaleria -> {
                            Atleta novaAtleta = new Atleta();
                            novaAtleta.setNome(dto.nome());
                            novaAtleta.setModalidade(dto.modalidade());
                            novaAtleta.setBiografia(dto.biografia());
                            novaAtleta.setCompeticao(dto.competicao());
                            novaAtleta.setFotos(fotosDaGaleria);

                            // ✅ LÓGICA DE DESTAQUE CORRIGIDA
                            definirFotoDestaque(novaAtleta, dto.fotos(), filesMap);

                            return directService.saveAtleta(novaAtleta);
                        })
        );
    }

    public Mono<Atleta> updateAtletaWithGallery(String id, AtletaFormDTO dto, Flux<FilePart> files) {
        return Mono.zip(atletaRepository.findById(id), files.collectMap(FilePart::filename).defaultIfEmpty(Map.of()))
                .flatMap(tuple -> {
                    Atleta atletaExistente = tuple.getT1();
                    Map<String, FilePart> filesMap = tuple.getT2();

                    return Flux.fromIterable(dto.fotos()).flatMap(fotoDto -> {
                        FilePart file = filesMap.get(fotoDto.filename());
                        if (file != null) {
                            return cloudStorageService.uploadFile(file)
                                    .map(url -> new FotoAcervo(url, fotoDto.legenda()));
                        } else if (fotoDto.url() != null && !fotoDto.url().isBlank()) {
                            FotoAcervo fotoExistente = new FotoAcervo(fotoDto.url(), fotoDto.legenda());
                            fotoExistente.setId(fotoDto.id());
                            return Mono.just(fotoExistente);
                        }
                        return Mono.empty();
                    }).collectList().flatMap(novaListaDeFotos -> {
                        atletaExistente.setNome(dto.nome());
                        atletaExistente.setModalidade(dto.modalidade());
                        atletaExistente.setBiografia(dto.biografia());
                        atletaExistente.setCompeticao(dto.competicao());
                        atletaExistente.setFotos(novaListaDeFotos);

                        // ✅ LÓGICA DE DESTAQUE CORRIGIDA
                        definirFotoDestaque(atletaExistente, dto.fotos(), filesMap);

                        return directService.saveAtleta(atletaExistente);
                    });
                });
    }

    /**
     * ✅ LÓGICA DE DESTAQUE ROBUSTA
     * Encontra e define a foto destaque na atleta usando uma chave confiável.
     */
    private void definirFotoDestaque(Atleta atleta, List<FotoDTO> fotosDto, Map<String, FilePart> filesMap) {
        atleta.getFotos().forEach(f -> f.setEhDestaque(false)); // Reseta todos os destaques

        Optional<FotoDTO> destaqueDtoOpt = fotosDto.stream().filter(FotoDTO::ehDestaque).findFirst();
        if (destaqueDtoOpt.isEmpty() && !fotosDto.isEmpty()) {
            destaqueDtoOpt = Optional.of(fotosDto.get(0)); // Fallback para o primeiro
        }

        destaqueDtoOpt.ifPresent(destaqueDto -> {
            Optional<FotoAcervo> fotoDeDestaqueOpt;
            if (destaqueDto.filename() != null && filesMap.containsKey(destaqueDto.filename())) {
                // A foto destaque é uma foto NOVA, identifica pela legenda/nome do arquivo
                fotoDeDestaqueOpt = atleta.getFotos().stream()
                        .filter(f -> f.getLegenda().equals(destaqueDto.legenda()))
                        .findFirst();
            } else {
                // A foto destaque é uma foto EXISTENTE, identifica pelo ID
                fotoDeDestaqueOpt = atleta.getFotos().stream()
                        .filter(f -> f.getId().equals(destaqueDto.id()))
                        .findFirst();
            }

            fotoDeDestaqueOpt.ifPresent(fotoDeDestaque -> {
                fotoDeDestaque.setEhDestaque(true);
                atleta.setFotoDestaqueId(fotoDeDestaque.getId());
            });
        });

        // Fallback final: se nada foi definido, define a primeira da lista
        if (atleta.getFotoDestaqueId() == null && !atleta.getFotos().isEmpty()) {
            FotoAcervo primeiraFoto = atleta.getFotos().get(0);
            primeiraFoto.setEhDestaque(true);
            atleta.setFotoDestaqueId(primeiraFoto.getId());
        }
    }
}