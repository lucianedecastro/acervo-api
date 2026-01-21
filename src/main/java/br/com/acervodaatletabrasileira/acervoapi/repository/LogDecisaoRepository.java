package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.LogDecisao;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface LogDecisaoRepository
        extends ReactiveMongoRepository<LogDecisao, String> {

    /* =====================================================
       CONSULTAS BÁSICAS
       ===================================================== */

    Flux<LogDecisao> findByTipoDecisao(
            TipoDecisao tipoDecisao
    );

    Flux<LogDecisao> findByEntidadeAndEntidadeId(
            String entidade,
            String entidadeId
    );

    Flux<LogDecisao> findByResponsavel(
            String responsavel
    );

    /* =====================================================
       CONSULTAS POR PERÍODO (AUDITORIA)
       ===================================================== */

    /**
     * Busca logs entre duas datas.
     * Essencial para auditoria, exportação e relatórios.
     */
    Flux<LogDecisao> findByDataDecisaoBetween(
            Instant inicio,
            Instant fim
    );

    /**
     * Busca logs a partir de uma data.
     */
    Flux<LogDecisao> findByDataDecisaoAfter(
            Instant inicio
    );

    /**
     * Busca logs até uma data.
     */
    Flux<LogDecisao> findByDataDecisaoBefore(
            Instant fim
    );
}
