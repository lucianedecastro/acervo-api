package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.DocumentoDireitos;
import br.com.acervodaatletabrasileira.acervoapi.repository.DocumentoDireitosRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Serviço responsável por centralizar as regras jurídicas
 * relacionadas ao uso, circulação e licenciamento do acervo.
 *
 * NÃO calcula valores
 * NÃO cria transações
 * NÃO conhece regras fiscais
 *
 * Apenas valida se o uso é juridicamente permitido.
 */
@Service
public class JuridicoService {

    private final DocumentoDireitosRepository documentoRepository;

    public JuridicoService(DocumentoDireitosRepository documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    /* =====================================================
       CONSULTAS BÁSICAS
       ===================================================== */

    public Flux<DocumentoDireitos> buscarPorItemAcervo(String itemAcervoId) {
        return documentoRepository.findByItemAcervoId(itemAcervoId);
    }

    public Mono<DocumentoDireitos> buscarPorFoto(String fotoPublicId) {
        return documentoRepository.findByFotoPublicId(fotoPublicId);
    }

    /* =====================================================
       VALIDAÇÃO CENTRAL
       ===================================================== */

    /**
     * Valida se um item do acervo pode ser licenciado,
     * considerando todos os documentos jurídicos vinculados.
     */
    public Mono<Boolean> podeLicenciarItem(String itemAcervoId) {
        return documentoRepository.findByItemAcervoId(itemAcervoId)
                .filter(DocumentoDireitos::permiteLicenciamento)
                .hasElements();
    }

    /**
     * Valida se uma foto específica pode ser licenciada.
     * Caso não haja documento específico da foto,
     * avalia os documentos do item como fallback.
     */
    public Mono<Boolean> podeLicenciarFoto(String itemAcervoId, String fotoPublicId) {
        return documentoRepository.findByFotoPublicId(fotoPublicId)
                .map(DocumentoDireitos::permiteLicenciamento)
                .switchIfEmpty(podeLicenciarItem(itemAcervoId));
    }

    /* =====================================================
       VALIDAÇÃO AVANÇADA (FINALIDADE / TERRITÓRIO)
       ===================================================== */

    public Mono<Boolean> podeUsarParaFinalidade(
            String itemAcervoId,
            DocumentoDireitos.FinalidadeUso finalidade
    ) {
        return documentoRepository.findByItemAcervoId(itemAcervoId)
                .filter(doc ->
                        doc.permiteLicenciamento()
                                && (doc.getFinalidadesPermitidas() == null
                                || doc.getFinalidadesPermitidas().contains(finalidade))
                )
                .hasElements();
    }

    public Mono<Boolean> podeUsarEmTerritorio(
            String itemAcervoId,
            DocumentoDireitos.TerritorioUso territorio
    ) {
        return documentoRepository.findByItemAcervoId(itemAcervoId)
                .filter(doc ->
                        doc.permiteLicenciamento()
                                && (doc.getTerritoriosPermitidos() == null
                                || doc.getTerritoriosPermitidos().contains(territorio))
                )
                .hasElements();
    }

    /* =====================================================
       AUDITORIA / MANUTENÇÃO
       ===================================================== */

    /**
     * Marca documentos expirados automaticamente.
     * Pode ser usado em job agendado no futuro.
     */
    public Mono<Long> expirarDocumentosVencidos() {
        return documentoRepository.findByValidoAteBefore(Instant.now())
                .filter(doc -> doc.getStatus() == DocumentoDireitos.StatusDocumentoDireitos.VALIDADO)
                .flatMap(doc -> {
                    doc.setStatus(DocumentoDireitos.StatusDocumentoDireitos.EXPIRADO);
                    doc.setAtualizadoEm(Instant.now());
                    return documentoRepository.save(doc);
                })
                .count();
    }

    /* =====================================================
       UTILITÁRIOS (SUPORTE A OUTROS SERVICES)
       ===================================================== */

    /**
     * Retorna os documentos válidos de um item,
     * útil para explicação de bloqueios no frontend/admin.
     */
    public Flux<DocumentoDireitos> listarDocumentosValidos(String itemAcervoId) {
        return documentoRepository.findByItemAcervoIdAndStatus(
                itemAcervoId,
                DocumentoDireitos.StatusDocumentoDireitos.VALIDADO
        );
    }
}

