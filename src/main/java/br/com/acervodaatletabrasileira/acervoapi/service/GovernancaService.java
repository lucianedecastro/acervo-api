package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.LogDecisao;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import br.com.acervodaatletabrasileira.acervoapi.repository.LogDecisaoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Serviço de Governança e Compliance.
 *
 * Responsável apenas por registrar e consultar decisões.
 * NÃO altera estado de domínio.
 */
@Service
public class GovernancaService {

    private final LogDecisaoRepository repository;

    public GovernancaService(LogDecisaoRepository repository) {
        this.repository = repository;
    }

    /* =====================================================
       REGISTRO
       ===================================================== */

    public Mono<LogDecisao> registrarDecisao(
            TipoDecisao tipo,
            String entidade,
            String entidadeId,
            String decisao,
            String justificativa,
            String responsavel,
            String roleResponsavel
    ) {
        LogDecisao log = new LogDecisao(
                null,
                tipo,
                entidade,
                entidadeId,
                decisao,
                justificativa,
                responsavel,
                roleResponsavel,
                Instant.now()
        );

        return repository.save(log);
    }

    /* =====================================================
       CONSULTAS
       ===================================================== */

    /**
     * Lista todos os logs de governança.
     * Uso exclusivo administrativo/auditoria.
     */
    public Flux<LogDecisao> listarTodos() {
        return repository.findAll();
    }

    public Flux<LogDecisao> listarPorEntidade(
            String entidade,
            String entidadeId
    ) {
        return repository.findByEntidadeAndEntidadeId(
                entidade,
                entidadeId
        );
    }

    public Flux<LogDecisao> listarPorTipo(
            TipoDecisao tipo
    ) {
        return repository.findByTipoDecisao(tipo);
    }

    public Flux<LogDecisao> listarPorResponsavel(
            String responsavel
    ) {
        return repository.findByResponsavel(responsavel);
    }

    /* =====================================================
       CONSULTA POR PERÍODO (AUDITORIA)
       ===================================================== */

    /**
     * Lista logs por período.
     *
     * - Se inicio e fim forem informados → filtra por intervalo
     * - Se apenas um for informado → aplica limite unilateral
     * - Se nenhum for informado → retorna todos
     */
    public Flux<LogDecisao> listarPorPeriodo(
            Instant inicio,
            Instant fim
    ) {

        if (inicio != null && fim != null) {
            return repository.findByDataDecisaoBetween(inicio, fim);
        }

        if (inicio != null) {
            return repository.findByDataDecisaoBetween(
                    inicio,
                    Instant.now()
            );
        }

        if (fim != null) {
            return repository.findByDataDecisaoBetween(
                    Instant.EPOCH,
                    fim
            );
        }

        return listarTodos();
    }
}
