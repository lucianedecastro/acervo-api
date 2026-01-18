package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface ItemAcervoRepository extends ReactiveMongoRepository<ItemAcervo, String> {

    // ==========================================
    // MÉTODOS DE CONTAGEM (Dashboard)
    // ==========================================

    Mono<Long> countByStatus(StatusItemAcervo status);

    Mono<Long> countByAtletasIdsContaining(String atletaId);

    Mono<Long> countByAtletasIdsContainingAndStatus(String atletaId, StatusItemAcervo status);

    // ==========================================
    // CONSULTAS DE LISTAGEM
    // ==========================================

    // Essencial para o nosso AtletaPerfilDTO (O Combo)
    Flux<ItemAcervo> findByAtletasIdsContaining(String atletaId);

    Flux<ItemAcervo> findByStatus(StatusItemAcervo status);

    Flux<ItemAcervo> findByStatusIn(Collection<StatusItemAcervo> statuses);

    Flux<ItemAcervo> findByAtletasIdsContainingAndStatus(String atletaId, StatusItemAcervo status);

    Flux<ItemAcervo> findByAtletasIdsContainingAndStatusIn(String atletaId, Collection<StatusItemAcervo> statuses);

    Flux<ItemAcervo> findByModalidadeId(String modalidadeId);

    Flux<ItemAcervo> findByModalidadeIdAndStatus(String modalidadeId, StatusItemAcervo status);

    Flux<ItemAcervo> findByProcedenciaContainingIgnoreCase(String termo);

    Flux<ItemAcervo> findByItemHistoricoTrue();
}