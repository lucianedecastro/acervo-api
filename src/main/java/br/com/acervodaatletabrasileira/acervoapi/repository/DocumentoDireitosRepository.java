package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.DocumentoDireitos;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface DocumentoDireitosRepository
        extends ReactiveMongoRepository<DocumentoDireitos, String> {

    /* =====================================================
       BUSCAS POR VÍNCULO
       ===================================================== */

    Flux<DocumentoDireitos> findByItemAcervoId(String itemAcervoId);

    Mono<DocumentoDireitos> findByFotoPublicId(String fotoPublicId);

    Flux<DocumentoDireitos> findByAtletasIdsContaining(String atletaId);

    Flux<DocumentoDireitos> findByAutorId(String autorId);

    /* =====================================================
       STATUS JURÍDICO
       ===================================================== */

    Flux<DocumentoDireitos> findByStatus(
            DocumentoDireitos.StatusDocumentoDireitos status
    );

    Flux<DocumentoDireitos> findByPermiteUsoComercialTrue();

    Flux<DocumentoDireitos> findByStatusAndPermiteUsoComercialTrue(
            DocumentoDireitos.StatusDocumentoDireitos status
    );

    /* =====================================================
       VIGÊNCIA / EXPIRAÇÃO
       ===================================================== */

    Flux<DocumentoDireitos> findByValidoAteBefore(Instant data);

    Flux<DocumentoDireitos> findByItemAcervoIdAndStatus(
            String itemAcervoId,
            DocumentoDireitos.StatusDocumentoDireitos status
    );

    /* =====================================================
       AUDITORIA E INTEGRIDADE (INCREMENTO)
       ===================================================== */

    Flux<DocumentoDireitos> findByResponsavelValidacao(
            String responsavelValidacao
    );

    /**
     * Busca um documento pelo seu Hash SHA-256 único.
     * Essencial para validar se o arquivo enviado é idêntico ao registrado.
     */
    Mono<DocumentoDireitos> findByHashConteudo(String hashConteudo);

    /**
     * Busca a prova de validade jurídica na Blockchain.
     */
    Mono<DocumentoDireitos> findByBlockchainTxId(String blockchainTxId);
}