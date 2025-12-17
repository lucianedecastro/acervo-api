package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AtletaService {

    private final AtletaRepository atletaRepository;

    public AtletaService(AtletaRepository atletaRepository) {
        this.atletaRepository = atletaRepository;
    }

    /* ==========================
       LEITURA (PÚBLICA)
       ========================== */

    public Flux<Atleta> findAll() {
        return atletaRepository.findAll();
    }

    public Mono<Atleta> findById(String id) {
        return atletaRepository.findById(id);
    }

    /* ==========================
       CRIAÇÃO (ADMIN)
       ========================== */

    public Mono<Atleta> create(AtletaFormDTO dto) {

        Atleta atleta = new Atleta();
        atleta.setNome(dto.nome());
        atleta.setModalidade(dto.modalidade());
        atleta.setBiografia(dto.biografia());
        atleta.setCompeticao(dto.competicao());
        atleta.setFotos(mapFotos(dto.fotos()));
        atleta.setFotoDestaqueId(dto.fotoDestaqueId());

        return atletaRepository.save(atleta);
    }

    /* ==========================
       ATUALIZAÇÃO (ADMIN)
       ========================== */

    public Mono<Atleta> update(String id, AtletaFormDTO dto) {

        return atletaRepository.findById(id)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("Atleta não encontrado"))
                )
                .flatMap(atletaExistente -> {
                    atletaExistente.setNome(dto.nome());
                    atletaExistente.setModalidade(dto.modalidade());
                    atletaExistente.setBiografia(dto.biografia());
                    atletaExistente.setCompeticao(dto.competicao());
                    atletaExistente.setFotos(mapFotos(dto.fotos()));
                    atletaExistente.setFotoDestaqueId(dto.fotoDestaqueId());
                    return atletaRepository.save(atletaExistente);
                });
    }

    /* ==========================
       DELETE (ADMIN)
       ========================== */

    public Mono<Void> deleteById(String id) {
        return atletaRepository.deleteById(id);
    }

    /* ==========================
       MAPPER DTO → MODEL
       ========================== */

    private List<FotoAcervo> mapFotos(List<FotoDTO> fotosDto) {

        if (fotosDto == null || fotosDto.isEmpty()) {
            return List.of();
        }

        return fotosDto.stream()
                .map(dto -> {
                    FotoAcervo foto = new FotoAcervo(
                            dto.url(),
                            dto.legenda()
                    );

                    if (dto.id() != null) {
                        foto.setId(dto.id());
                    }

                    foto.setEhDestaque(Boolean.TRUE.equals(dto.ehDestaque()));

                    return foto;
                })
                .toList();
    }
}
