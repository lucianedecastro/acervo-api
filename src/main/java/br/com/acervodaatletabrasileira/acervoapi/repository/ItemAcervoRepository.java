package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusBlockchain;
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
       MEMORIAL / PESQUISA (ORIGINAL)
       ===================================================== */

    Flux<ItemAcervo> findByItemHistoricoTrue();

    /* =====================================================
       BLOCKCHAIN & AUDITORIA (INCREMENTO)
       ===================================================== */

    /**
     * Busca itens que falharam ou estão pendentes de registro na rede.
     * Útil para Jobs de retentativa.
     */
    Flux<ItemAcervo> findByStatusBlockchain(StatusBlockchain statusBlockchain);

    /**
     * Busca item específico pelo Hash da Transação (Recibo Público).
     */
    Mono<ItemAcervo> findByBlockchainTxId(String blockchainTxId);

    /* =====================================================
       PESQUISA AVANÇADA / FONTES (INCREMENTO)
       ===================================================== */

    /**
     * Filtra apenas itens em Domínio Público (Isentos de Royalties).
     */
    Flux<ItemAcervo> findByDominioPublicoTrue();

    /**
     * Busca por fonte de pesquisa (ex: "Jornal O Paiz", "Arquivo Nacional").
     */
    Flux<ItemAcervo> findByFontePesquisaContainingIgnoreCase(String fontePesquisa);
}
