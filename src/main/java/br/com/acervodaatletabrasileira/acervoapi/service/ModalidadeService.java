package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;

@Service
public class ModalidadeService {

    private final ModalidadeRepository repository;

    public ModalidadeService(ModalidadeRepository repository) {
        this.repository = repository;
    }

    /* ==========================
       LEITURA (PÚBLICA)
       ========================== */

    public Flux<Modalidade> findAll() {
        return repository.findAll()
                .filter(Modalidade::getAtiva);
    }

    public Mono<Modalidade> findById(String id) {
        return repository.findById(id)
                .filter(Modalidade::getAtiva);
    }

    /* ==========================
       CRIAÇÃO (ADMIN)
       ========================== */

    public Mono<Modalidade> create(ModalidadeDTO dto) {

        Modalidade modalidade = new Modalidade();
        modalidade.setNome(dto.nome());
        modalidade.setHistoria(dto.historia());
        modalidade.setPictogramaUrl(dto.pictogramaUrl());
        modalidade.setAtiva(dto.ativa() != null ? dto.ativa() : true);

        modalidade.setSlug(generateSlug(dto.nome()));
        modalidade.setCriadoEm(Instant.now());
        modalidade.setAtualizadoEm(Instant.now());

        return repository.save(modalidade);
    }

    /* ==========================
       ATUALIZAÇÃO (ADMIN)
       ========================== */

    public Mono<Modalidade> update(String id, ModalidadeDTO dto) {
        return repository.findById(id)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("Modalidade não encontrada"))
                )
                .flatMap(existing -> {

                    existing.setNome(dto.nome());
                    existing.setHistoria(dto.historia());
                    existing.setPictogramaUrl(dto.pictogramaUrl());
                    existing.setAtiva(dto.ativa() != null ? dto.ativa() : existing.getAtiva());

                    existing.setSlug(generateSlug(dto.nome()));
                    existing.setAtualizadoEm(Instant.now());

                    return repository.save(existing);
                });
    }

    /* ==========================
       DELETE (ADMIN)
       ========================== */

    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    /* ==========================
       UTIL
       ========================== */

    private String generateSlug(String input) {
        if (input == null) return null;

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
