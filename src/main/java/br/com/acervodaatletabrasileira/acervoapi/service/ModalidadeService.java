package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        return repository.findAll();
    }

    public Mono<Modalidade> findById(String id) {
        return repository.findById(id);
    }

    /* ==========================
       CRIAÇÃO (ADMIN)
       ========================== */

    public Mono<Modalidade> save(Modalidade modalidade) {
        return repository.save(modalidade);
    }

    public Mono<Modalidade> create(ModalidadeDTO dto) {
        Modalidade modalidade = new Modalidade();
        modalidade.setNome(dto.nome());
        modalidade.setHistoria(dto.historia());

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
                    return repository.save(existing);
                });
    }

    /* ==========================
       DELETE (ADMIN)
       ========================== */

    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }
}
