package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Licenciamento;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LicenciamentoRepository
        extends ReactiveMongoRepository<Licenciamento, String> {

    Flux<Licenciamento> findByItemAcervoId(String itemAcervoId);

    Flux<Licenciamento> findByAtletaId(String atletaId);

    /* =====================================================
       AUDITORIA E PROVA DE USO (INCREMENTO)
       ===================================================== */

    /**
     * Busca um licenciamento pelo Hash da Transação na Blockchain.
     * Serve como o "Certificado de Autenticidade" público.
     */
    Mono<Licenciamento> findByBlockchainTxId(String blockchainTxId);

    /**
     * Busca licenças por tipo de uso (ex: COMERCIAL, EDITORIAL).
     * Útil para relatórios de impacto e uso do acervo.
     */
    Flux<Licenciamento> findByTipoUso(String tipoUso);

    /* NOTA: Removido findByCompradorId para alinhar com a arquitetura
       do sistema, onde não há módulo de login para compradores.
    */
}