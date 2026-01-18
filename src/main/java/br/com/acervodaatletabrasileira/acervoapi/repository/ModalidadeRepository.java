package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ModalidadeRepository
        extends ReactiveMongoRepository<Modalidade, String> {

    /**
     * Busca modalidades pelo nome (case-insensitive).
     * Usado para filtros públicos e curadoria do acervo.
     */
    Flux<Modalidade> findByNomeContainingIgnoreCase(String nome);

    /**
     * Busca uma modalidade específica pelo seu slug único.
     * Essencial para construção de URLs amigáveis no frontend.
     */
    Mono<Modalidade> findBySlug(String slug);

    /**
     * Lista apenas as modalidades que estão marcadas como ativas.
     * Usado para popular menus e galerias públicas.
     */
    Flux<Modalidade> findByAtivaTrue();
}