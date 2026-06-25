package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.model.DocumentoDireitos;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import br.com.acervodaatletabrasileira.acervoapi.service.GovernancaService;
import br.com.acervodaatletabrasileira.acervoapi.service.BlockchainService;
import br.com.acervodaatletabrasileira.acervoapi.repository.DocumentoDireitosRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * =====================================================
 * JURÍDICO CONTROLLER
 * =====================================================
 *
 * RESPONSABILIDADE:
 * - Validação jurídica
 * - Registro de decisão interna (Governança)
 * - Registro de prova institucional externa (Blockchain)
 *
 * IMPORTANTE:
 * A Blockchain aqui NÃO representa pagamento,
 * apenas prova institucional de validação jurídica.
 */
@RestController
@RequestMapping("/juridico")
@Tag(
        name = "Jurídico / Governança",
        description = "Validação jurídica, cessão de direitos e auditoria institucional."
)
@SecurityRequirement(name = "bearerAuth")
public class JuridicoController {

    private final DocumentoDireitosRepository documentoRepository;
    private final GovernancaService governancaService;
    private final BlockchainService blockchainService;

    public JuridicoController(
            DocumentoDireitosRepository documentoRepository,
            GovernancaService governancaService,
            BlockchainService blockchainService
    ) {
        this.documentoRepository = documentoRepository;
        this.governancaService = governancaService;
        this.blockchainService = blockchainService;
    }

    /* =====================================================
       CONSULTAS ADMIN
       ===================================================== */

    @GetMapping("/documentos")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<DocumentoDireitos> listarTodos() {
        return documentoRepository.findAll();
    }

    @GetMapping("/documentos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<DocumentoDireitos>> buscarPorId(@PathVariable String id) {
        return documentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       VALIDAÇÃO JURÍDICA COM PROVA INSTITUCIONAL
       ===================================================== */

    @PatchMapping("/documentos/{id}/validar")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<DocumentoDireitos>> validarDocumento(
            @PathVariable String id,
            @RequestParam(required = false) String observacoes,
            Authentication authentication
    ) {

        String responsavel = authentication.getName();
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return documentoRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Documento não encontrado")))
                .flatMap(doc -> {

                    if (doc.getHashConteudo() == null || doc.getHashConteudo().isBlank()) {
                        return Mono.error(new IllegalStateException("Documento não possui hash de integridade"));
                    }

                    doc.setStatus(DocumentoDireitos.StatusDocumentoDireitos.VALIDADO);
                    doc.setObservacoesJuridico(observacoes);
                    doc.setResponsavelValidacao(responsavel);
                    doc.setAtualizadoEm(Instant.now());

                    return documentoRepository.save(doc)
                            .flatMap(saved ->

                                    // 1️⃣ Registro interno (Governança)
                                    governancaService.registrarDecisao(
                                                    TipoDecisao.JURIDICA,
                                                    "DOCUMENTO_DIREITOS",
                                                    saved.getId(),
                                                    "VALIDADO",
                                                    observacoes,
                                                    responsavel,
                                                    role
                                            )

                                            // 2️⃣ Registro externo (Prova Institucional Blockchain)
                                            .then(
                                                    blockchainService.registrarProvaInstitucional(
                                                            "DOCUMENTO_DIREITOS",
                                                            saved.getId(),
                                                            saved.getHashConteudo()
                                                    )
                                            )

                                            // 3️⃣ Persistência do TxId e timestamp institucional
                                            .flatMap(txId -> {
                                                saved.setBlockchainTxId(txId);
                                                saved.setDataRegistroBlockchain(Instant.now());
                                                return documentoRepository.save(saved);
                                            })
                            );
                })
                .map(ResponseEntity::ok);
    }

    /* =====================================================
       REJEIÇÃO JURÍDICA
       ===================================================== */

    @PatchMapping("/documentos/{id}/rejeitar")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<DocumentoDireitos>> rejeitarDocumento(
            @PathVariable String id,
            @RequestParam String motivo,
            Authentication authentication
    ) {

        String responsavel = authentication.getName();
        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return documentoRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Documento não encontrado")))
                .flatMap(doc -> {

                    doc.setStatus(DocumentoDireitos.StatusDocumentoDireitos.REJEITADO);
                    doc.setObservacoesJuridico(motivo);
                    doc.setResponsavelValidacao(responsavel);
                    doc.setAtualizadoEm(Instant.now());

                    return documentoRepository.save(doc)
                            .flatMap(saved ->
                                    governancaService.registrarDecisao(
                                            TipoDecisao.JURIDICA,
                                            "DOCUMENTO_DIREITOS",
                                            saved.getId(),
                                            "REJEITADO",
                                            motivo,
                                            responsavel,
                                            role
                                    ).thenReturn(saved)
                            );
                })
                .map(ResponseEntity::ok);
    }

    /* =====================================================
       FILTROS ADMIN
       ===================================================== */

    @GetMapping("/documentos/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<DocumentoDireitos> listarPorStatus(
            @PathVariable DocumentoDireitos.StatusDocumentoDireitos status
    ) {
        return documentoRepository.findByStatus(status);
    }
}
