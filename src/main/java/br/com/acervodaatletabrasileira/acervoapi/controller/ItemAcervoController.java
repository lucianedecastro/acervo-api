package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoCreateDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.service.ItemAcervoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/acervo")
@Tag(
        name = "Acervo da Atleta Brasileira",
        description = """
                Infraestrutura de preservação, pesquisa histórica e licenciamento.
                
                - Pesquisa Histórica: Itens de memória (Status MEMORIAL).
                - Justiça Financeira: Itens para licenciamento remunerado.
                - Curadoria Digital: Gestão de procedência e arquivos Cloudinary.
                """
)
public class ItemAcervoController {

    private final ItemAcervoService service;
    private final ObjectMapper objectMapper;

    public ItemAcervoController(ItemAcervoService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /* =====================================================
       CONSULTA PÚBLICA (VALORIZANDO A MEMÓRIA ATLETA)
       ===================================================== */

    @Operation(summary = "Lista itens públicos (Pesquisa Histórica e Licenciáveis)")
    @GetMapping
    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return service.listarPublicados();
    }

    @Operation(summary = "Busca detalhe de um item (Histórico ou Comercial)")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ItemAcervoResponseDTO>> buscarPorId(@PathVariable String id) {
        return service.buscarPublicadoPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Filtra o acervo pessoal de uma atleta específica")
    @GetMapping("/atleta/{atletaId}")
    public Flux<ItemAcervoResponseDTO> listarPorAtleta(@PathVariable String atletaId) {
        return service.listarPublicadosPorAtleta(atletaId);
    }

    @Operation(summary = "Filtra o acervo por uma modalidade esportiva específica")
    @GetMapping("/modalidade/{modalidadeId}")
    public Flux<ItemAcervoResponseDTO> listarPorModalidade(@PathVariable String modalidadeId) {
        return service.listarPublicadosPorModalidade(modalidadeId);
    }

    /* =====================================================
       ADMIN / CURADORIA / ÁREA DA ATLETA (ESCRITA)
       ===================================================== */

    @Operation(
            summary = "Lista base total para curadores (inclui rascunhos)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<ItemAcervo> listarTodos() {
        return service.listarTodos();
    }

    @Operation(
            summary = "Registra novo item (Marcando como Histórico ou Ativo)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Mono<ItemAcervo> criar(@RequestBody ItemAcervoCreateDTO dto) {
        return service.criar(dto);
    }

    @Operation(
            summary = "Atualiza metadados e regras de uso do item (Protegido por Role e Dono)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Mono<ResponseEntity<ItemAcervo>> atualizar(
            @PathVariable String id,
            @RequestBody ItemAcervoCreateDTO dto,
            Authentication authentication
    ) {
        // authentication.getName() retorna o e-mail da atleta ou admin
        String identificador = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return service.atualizarProtegido(id, dto, identificador, role)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Publica item (Itens históricos vão para MEMORIAL, ativos para PUBLICADO)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ItemAcervo>> publicar(@PathVariable String id) {
        return service.publicar(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Remove item do acervo digital",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> remover(@PathVariable String id) {
        return service.remover(id);
    }

    /* =====================================================
       GESTÃO DE ARQUIVOS DIGITAIS (CLOUDINARY)
       ===================================================== */

    @Operation(
            summary = "Upload de mídia bruta para o Cloudinary",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Mono<ResponseEntity<FotoDTO>> uploadAvulso(@RequestPart("file") FilePart file) {
        return service.uploadCloudinaryPuro(file).map(ResponseEntity::ok);
    }

    @Operation(
            summary = "Vincula foto ao item com legenda e destaque",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Mono<ResponseEntity<FotoDTO>> uploadFoto(
            @PathVariable String id,
            @RequestPart("file") FilePart file,
            @RequestPart("metadata") String metadataStr
    ) {
        return Mono.fromCallable(() -> objectMapper.readValue(metadataStr, FotoDTO.class))
                .flatMap(metadata -> service.adicionarFoto(id, file, metadata))
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Erro ao processar upload de foto no item {}: {}", id, e.getMessage()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()));
    }
}