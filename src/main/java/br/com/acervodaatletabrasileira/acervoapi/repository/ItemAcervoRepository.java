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

    /* =====================================================
       CONTAGENS (DASHBOARD / MÉTRICAS)
       ===================================================== */

    Mono<Long> countByStatus(StatusItemAcervo status);

    Mono<Long> countByAtletasIdsContaining(String atletaId);

    Mono<Long> countByAtletasIdsContainingAndStatus(String atletaId, StatusItemAcervo status);

    /* =====================================================
       CONSULTAS POR ATLETA
       ===================================================== */

    // Usado no perfil da atleta (combo)
    Flux<ItemAcervo> findByAtletasIdsContaining(String atletaId);

    Flux<ItemAcervo> findByAtletasIdsContainingAndStatus(
            String atletaId,
            StatusItemAcervo status
    );

    Flux<ItemAcervo> findByAtletasIdsContainingAndStatusIn(
            String atletaId,
            Collection<StatusItemAcervo> statuses
    );

    /* =====================================================
       CONSULTAS POR STATUS
       ===================================================== */

    Flux<ItemAcervo> findByStatus(StatusItemAcervo status);

    Flux<ItemAcervo> findByStatusIn(Collection<StatusItemAcervo> statuses);

    /**
     * Consulta semântica para itens públicos
     * (PUBLICADO, DISPONIVEL_LICENCIAMENTO, MEMORIAL)
     */
    Flux<ItemAcervo> findByStatusInAndItemHistoricoFalse(Collection<StatusItemAcervo> statuses);

    /* =====================================================
       CONSULTAS POR MODALIDADE
       ===================================================== */

    Flux<ItemAcervo> findByModalidadeId(String modalidadeId);

    Flux<ItemAcervo> findByModalidadeIdAndStatus(
            String modalidadeId,
            StatusItemAcervo status
    );

    /* =====================================================
       LICENCIAMENTO / MARKETPLACE
       ===================================================== */

    /**
     * Itens prontos para licenciamento comercial
     */
    Flux<ItemAcervo> findByDisponivelParaLicenciamentoTrueAndStatus(
            StatusItemAcervo status
    );

    /* =====================================================
       BUSCA EDITORIAL
       ===================================================== */

    Flux<ItemAcervo> findByProcedenciaContainingIgnoreCase(String termo);

    Flux<ItemAcervo> findByCreditoAutoralContainingIgnoreCase(String termo);

    /* =====================================================
       MEMORIAL / PESQUISA
       ===================================================== */

    Flux<ItemAcervo> findByItemHistoricoTrue();
}
