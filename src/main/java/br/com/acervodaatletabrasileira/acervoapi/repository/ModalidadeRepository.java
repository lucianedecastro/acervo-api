package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ModalidadeRepository
        extends ReactiveMongoRepository<Modalidade, String> {

    /**
     * Busca modalidades pelo nome (case-insensitive).
     * Usado para filtros públicos e curadoria do acervo.
     */
    Flux<Modalidade> findByNomeContainingIgnoreCase(String nome);
}
