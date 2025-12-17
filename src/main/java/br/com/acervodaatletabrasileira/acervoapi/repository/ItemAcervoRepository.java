package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ItemAcervoRepository
        extends ReactiveMongoRepository<ItemAcervo, String> {

    /* ==========================
       CONSULTAS PÚBLICAS
       ========================== */

    // Feed geral do acervo (apenas publicados)
    Flux<ItemAcervo> findByStatus(StatusItemAcervo status);

    // Itens publicados por atleta
    Flux<ItemAcervo> findByAtletasIdsContainingAndStatus(
            String atletaId,
            StatusItemAcervo status
    );

    // Itens publicados por modalidade
    Flux<ItemAcervo> findByModalidadeIdAndStatus(
            String modalidadeId,
            StatusItemAcervo status
    );

    /* ==========================
       CONSULTAS ADMIN / CURADORIA
       ========================== */

    // Todos os itens de um atleta (rascunho + publicado)
    Flux<ItemAcervo> findByAtletasIdsContaining(String atletaId);

    // Todos os itens de uma modalidade (rascunho + publicado)
    Flux<ItemAcervo> findByModalidadeId(String modalidadeId);
}
