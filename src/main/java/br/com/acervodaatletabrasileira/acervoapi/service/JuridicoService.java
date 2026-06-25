package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.DocumentoDireitos;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.DocumentoDireitosRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Serviço responsável por centralizar as regras jurídicas
 * relacionadas ao uso, circulação e licenciamento do acervo.
 *
 * Incremento: Agora valida também o status contratual da Atleta
 * e regras de Domínio Público.
 */
@Service
public class JuridicoService {

    private final DocumentoDireitosRepository documentoRepository;
    private final AtletaRepository atletaRepository;
    private final ItemAcervoRepository itemAcervoRepository;

    public JuridicoService(DocumentoDireitosRepository documentoRepository,
                           AtletaRepository atletaRepository,
                           ItemAcervoRepository itemAcervoRepository) {
        this.documentoRepository = documentoRepository;
        this.atletaRepository = atletaRepository;
        this.itemAcervoRepository = itemAcervoRepository;
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
       VALIDAÇÃO CENTRAL (ATUALIZADA)
       ===================================================== */

    /**
     * Valida se um item do acervo pode ser licenciado.
     * * Regras de Incremento:
     * 1. Se for DOMÍNIO PÚBLICO, o licenciamento é permitido (fins de preservação).
     * 2. Se houver Atletas vinculadas, TODAS devem ter Contrato Assinado e Identidade Verificada.
     * 3. Deve haver pelo menos um documento de direitos válido que permita o licenciamento.
     */
    public Mono<Boolean> podeLicenciarItem(String itemAcervoId) {
        return itemAcervoRepository.findById(itemAcervoId)
                .flatMap(item -> {
                    // Regra 1: Domínio Público é liberado juridicamente para o Acervo
                    if (Boolean.TRUE.equals(item.getDominioPublico())) {
                        return Mono.just(true);
                    }

                    // Regra 2: Validação das Atletas Vinculadas
                    return validarStatusContratualAtletas(item.getAtletasIds())
                            .flatMap(atletasOk -> {
                                if (!atletasOk) return Mono.just(false);

                                // Regra 3: Validação dos Documentos de Direitos do Item
                                return documentoRepository.findByItemAcervoId(itemAcervoId)
                                        .filter(DocumentoDireitos::permiteLicenciamento)
                                        .hasElements();
                            });
                })
                .defaultIfEmpty(false);
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
       VALIDAÇÃO DE STATUS DA ATLETA (NOVO)
       ===================================================== */

    /**
     * Verifica se todas as atletas na lista possuem conformidade jurídica.
     */
    private Mono<Boolean> validarStatusContratualAtletas(List<String> atletasIds) {
        if (atletasIds == null || atletasIds.isEmpty()) {
            return Mono.just(true); // Item sem atleta (ex: paisagem histórica)
        }

        return atletaRepository.findAllById(atletasIds)
                .collectList()
                .map(atletas -> atletas.stream().allMatch(atleta ->
                        Boolean.TRUE.equals(atleta.getContratoAssinado()) &&
                                atleta.getStatusVerificacao() == Atleta.StatusVerificacao.VERIFICADO
                ));
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