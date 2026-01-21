package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.model.DocumentoDireitos;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import br.com.acervodaatletabrasileira.acervoapi.service.GovernancaService;
import br.com.acervodaatletabrasileira.acervoapi.repository.DocumentoDireitosRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/juridico")
@Tag(
        name = "Jurídico / Governança",
        description = "Validação jurídica, cessão de direitos e auditoria do acervo"
)
@SecurityRequirement(name = "bearerAuth")
public class JuridicoController {

    private final DocumentoDireitosRepository documentoRepository;
    private final GovernancaService governancaService;

    public JuridicoController(
            DocumentoDireitosRepository documentoRepository,
            GovernancaService governancaService
    ) {
        this.documentoRepository = documentoRepository;
        this.governancaService = governancaService;
    }

    /* =====================================================
       CONSULTAS (ADMIN)
       ===================================================== */

    @Operation(summary = "Lista todos os documentos jurídicos")
    @GetMapping("/documentos")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<DocumentoDireitos> listarTodos() {
        return documentoRepository.findAll();
    }

    @Operation(summary = "Busca documento jurídico por ID")
    @GetMapping("/documentos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<DocumentoDireitos>> buscarPorId(
            @PathVariable String id
    ) {
        return documentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       DECISÃO JURÍDICA (ADMIN)
       ===================================================== */

    @Operation(summary = "Valida documento jurídico")
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

                    doc.setStatus(DocumentoDireitos.StatusDocumentoDireitos.VALIDADO);
                    doc.setObservacoesJuridico(observacoes);
                    doc.setResponsavelValidacao(responsavel);
                    doc.setAtualizadoEm(Instant.now());

                    return documentoRepository.save(doc)
                            .flatMap(saved ->
                                    governancaService.registrarDecisao(
                                            TipoDecisao.JURIDICA,
                                            "DOCUMENTO_DIREITOS",
                                            saved.getId(),
                                            "VALIDADO",
                                            observacoes,
                                            responsavel,
                                            role
                                    ).thenReturn(saved)
                            );
                })
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Rejeita documento jurídico")
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
       AUDITORIA (ADMIN)
       ===================================================== */

    @Operation(summary = "Lista documentos jurídicos por status")
    @GetMapping("/documentos/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<DocumentoDireitos> listarPorStatus(
            @PathVariable DocumentoDireitos.StatusDocumentoDireitos status
    ) {
        return documentoRepository.findByStatus(status);
    }
}

