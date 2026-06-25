package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPerfilDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPublicoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SubcontaPagamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.DocumentoLegalService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/atletas")
@Tag(name = "Atletas", description = "Endpoints públicos e administrativos do acervo de atletas")
public class AtletaController {

    private final AtletaService atletaService;
    private final DocumentoLegalService documentoLegalService;

    public AtletaController(AtletaService atletaService,
                            DocumentoLegalService documentoLegalService) {
        this.atletaService = atletaService;
        this.documentoLegalService = documentoLegalService;
    }

    /* =====================================================
       DASHBOARD DA ATLETA (LOGADA)
       ===================================================== */

    @Operation(
            summary = "Retorna o perfil da atleta logada (Dashboard)",
            description = "Extrai a identidade da atleta a partir do Token JWT (suporta ID ou E-mail no subject).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public Mono<ResponseEntity<Atleta>> obterMeuPerfil(Principal principal) {

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String identificador = principal.getName().trim();

        return atletaService.findById(identificador)
                .switchIfEmpty(atletaService.findByEmail(identificador))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /* =====================================================
       LEITURA PÚBLICA (COM DTO DE PROTEÇÃO)
       ===================================================== */

    @Operation(summary = "Lista atletas do acervo (visão pública protegida)")
    @GetMapping
    public Flux<AtletaPublicoDTO> listar(
            @Parameter(description = "Filtrar por: HISTORICA, ATIVA ou ESPOLIO")
            @RequestParam(required = false) Atleta.CategoriaAtleta categoria
    ) {

        Flux<Atleta> atletas = (categoria != null)
                ? atletaService.findAll().filter(atleta -> atleta.getCategoria() == categoria)
                : atletaService.findAll();

        return atletas.map(AtletaPublicoDTO::fromModel);
    }

    @Operation(summary = "Busca os dados públicos de uma atleta pelo ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AtletaPublicoDTO>> buscarPorId(@PathVariable String id) {
        return atletaService.findById(id)
                .map(atleta -> ResponseEntity.ok(AtletaPublicoDTO.fromModel(atleta)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Busca o perfil completo da atleta pelo Slug (Dados + Acervo)")
    @GetMapping("/perfil/{slug}")
    public Mono<ResponseEntity<AtletaPerfilDTO>> buscarPorSlug(@PathVariable String slug) {
        return atletaService.getPerfilCompletoBySlug(slug)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       ADMIN – ESCRITA E VERIFICAÇÃO (BLINDAGEM INSTITUCIONAL)
       ===================================================== */

    @Operation(
            summary = "Cadastra uma nova atleta",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atleta> criar(@RequestBody AtletaFormDTO dto) {
        return atletaService.create(dto);
    }

    @Operation(
            summary = "Atualiza uma atleta existente",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Atleta>> atualizar(
            @PathVariable String id,
            @RequestBody AtletaFormDTO dto
    ) {
        return atletaService.update(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Aprova ou rejeita a verificação de identidade/legalidade",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/verificacao")
    public Mono<ResponseEntity<Atleta>> verificarAtleta(
            @PathVariable String id,
            @RequestParam Atleta.StatusVerificacao status,
            @RequestParam(required = false) String observacoes
    ) {
        return atletaService.verificarAtleta(id, status, observacoes)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Hidden
    @Operation(
            summary = "Provisiona a conta de recebimento (Asaas) da atleta",
            description = "Cria a subconta no gateway de pagamento e salva o walletId para uso no split. " +
                    "Exige identidade já verificada.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/gateway/subconta")
    public Mono<ResponseEntity<Atleta>> provisionarContaPagamento(
            @PathVariable String id,
            @RequestBody SubcontaPagamentoDTO dto
    ) {
        return atletaService.provisionarContaPagamento(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Remove uma atleta do acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> remover(@PathVariable String id) {
        return atletaService.deleteById(id)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
                .onErrorResume(e -> Mono.just(new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED)));
    }

    /* =====================================================
       INFRAESTRUTURA JURÍDICA
       ===================================================== */

    @Operation(
            summary = "Upload de Documentos Jurídicos (Identidade / Contrato)",
            description = "Processa o arquivo, gera hash de integridade e salva em storage seguro.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Atleta>> uploadDocumentoLegal(
            @PathVariable String id,
            @RequestPart("file") FilePart file,
            @RequestParam("tipo") DocumentoLegalService.TipoDocumento tipo
    ) {

        return documentoLegalService.processarDocumentoLegal(file, id, tipo)
                .flatMap(metadata -> atletaService.vincularDocumentoLegal(id, metadata, tipo))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}