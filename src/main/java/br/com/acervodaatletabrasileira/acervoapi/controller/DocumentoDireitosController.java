package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.model.DocumentoDireitos;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import br.com.acervodaatletabrasileira.acervoapi.service.GovernancaService;
import br.com.acervodaatletabrasileira.acervoapi.repository.DocumentoDireitosRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/admin/juridico/documentos")
@Tag(
        name = "Jurídico / Documentos de Direitos",
        description = "Gestão administrativa de documentos jurídicos de direitos autorais e imagem"
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class DocumentoDireitosController {

    private final DocumentoDireitosRepository repository;
    private final GovernancaService governancaService;

    public DocumentoDireitosController(
            DocumentoDireitosRepository repository,
            GovernancaService governancaService
    ) {
        this.repository = repository;
        this.governancaService = governancaService;
    }

    /* =====================================================
       LISTAGEM
       ===================================================== */

    @Operation(summary = "Lista todos os documentos jurídicos")
    @GetMapping
    public Flux<DocumentoDireitos> listarTodos() {
        return repository.findAll();
    }

    @Operation(summary = "Busca documento jurídico por ID")
    @GetMapping("/{id}")
    public Mono<DocumentoDireitos> buscarPorId(
            @PathVariable String id
    ) {
        return repository.findById(id);
    }

    /* =====================================================
       CRIAÇÃO
       ===================================================== */

    @Operation(summary = "Cria um novo documento jurídico")
    @PostMapping
    public Mono<DocumentoDireitos> criar(
            @RequestBody DocumentoDireitos documento,
            Principal principal
    ) {
        documento.setCriadoEm(Instant.now());
        documento.setAtualizadoEm(Instant.now());
        documento.setStatus(
                documento.getStatus() != null
                        ? documento.getStatus()
                        : DocumentoDireitos.StatusDocumentoDireitos.PENDENTE_ANALISE
        );

        return repository.save(documento)
                .flatMap(saved ->
                        governancaService.registrarDecisao(
                                br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao.JURIDICA,
                                "DocumentoDireitos",
                                saved.getId(),
                                "CRIACAO",
                                "Documento jurídico cadastrado",
                                principal.getName(),
                                "ROLE_ADMIN"
                        ).thenReturn(saved)
                );
    }

    /* =====================================================
       ATUALIZAÇÃO
       ===================================================== */

    @Operation(summary = "Atualiza um documento jurídico existente")
    @PutMapping("/{id}")
    public Mono<DocumentoDireitos> atualizar(
            @PathVariable String id,
            @RequestBody DocumentoDireitos atualizado,
            Principal principal
    ) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Documento não encontrado")))
                .flatMap(existente -> {

                    atualizado.setId(existente.getId());
                    atualizado.setCriadoEm(existente.getCriadoEm());
                    atualizado.setAtualizadoEm(Instant.now());

                    return repository.save(atualizado);
                })
                .flatMap(saved ->
                        governancaService.registrarDecisao(
                                br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao.JURIDICA,
                                "DocumentoDireitos",
                                saved.getId(),
                                "ATUALIZACAO",
                                "Documento jurídico atualizado",
                                principal.getName(),
                                "ROLE_ADMIN"
                        ).thenReturn(saved)
                );
    }

    /* =====================================================
       ALTERAÇÃO DE STATUS (FLUXO JURÍDICO)
       ===================================================== */

    @Operation(summary = "Atualiza o status jurídico do documento")
    @PatchMapping("/{id}/status")
    public Mono<DocumentoDireitos> atualizarStatus(
            @PathVariable String id,
            @RequestParam DocumentoDireitos.StatusDocumentoDireitos status,
            @RequestParam(required = false) String observacoes,
            Principal principal
    ) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Documento não encontrado")))
                .flatMap(doc -> {
                    doc.setStatus(status);
                    doc.setObservacoesJuridico(observacoes);
                    doc.setResponsavelValidacao(principal.getName());
                    doc.setAtualizadoEm(Instant.now());
                    return repository.save(doc);
                })
                .flatMap(saved ->
                        governancaService.registrarDecisao(
                                br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao.JURIDICA,
                                "DocumentoDireitos",
                                saved.getId(),
                                "ALTERACAO_STATUS",
                                "Status alterado para " + status,
                                principal.getName(),
                                "ROLE_ADMIN"
                        ).thenReturn(saved)
                );
    }

    /* =====================================================
       REMOÇÃO (EXCEPCIONAL)
       ===================================================== */

    @Operation(summary = "Remove um documento jurídico (uso excepcional)")
    @DeleteMapping("/{id}")
    public Mono<Void> remover(
            @PathVariable String id,
            Principal principal
    ) {
        return repository.findById(id)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("Documento não encontrado"))
                )
                .flatMap(doc ->
                        repository.deleteById(id)
                                .then(
                                        governancaService.registrarDecisao(
                                                TipoDecisao.JURIDICA,
                                                "DocumentoDireitos",
                                                id,
                                                "REMOCAO",
                                                "Documento jurídico removido",
                                                principal.getName(),
                                                "ROLE_ADMIN"
                                        )
                                )
                )
                .then(); // 🔑 converte Mono<LogDecisao> → Mono<Void>
    }
}

