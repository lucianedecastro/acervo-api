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

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/acervo")
@Tag(
        name = "Acervo da Atleta Brasileira",
        description = """
                Infraestrutura de preservação, pesquisa histórica e licenciamento.

                - Pesquisa Histórica
                - Curadoria Digital
                - Licenciamento Jurídico e Financeiro
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
       CONSULTA PÚBLICA
       ===================================================== */

    @Operation(summary = "Lista itens públicos (Históricos e Licenciáveis)")
    @GetMapping
    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return service.listarPublicados();
    }

    @Operation(summary = "Busca detalhe de um item público")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ItemAcervoResponseDTO>> buscarPorId(@PathVariable String id) {
        return service.buscarPublicadoPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Lista itens públicos de uma atleta")
    @GetMapping("/atleta/{atletaId}")
    public Flux<ItemAcervoResponseDTO> listarPorAtleta(@PathVariable String atletaId) {
        return service.listarPublicadosPorAtleta(atletaId);
    }

    @Operation(summary = "Lista itens públicos por modalidade")
    @GetMapping("/modalidade/{modalidadeId}")
    public Flux<ItemAcervoResponseDTO> listarPorModalidade(@PathVariable String modalidadeId) {
        return service.listarPublicadosPorModalidade(modalidadeId);
    }

    /* =====================================================
       ADMIN / CURADORIA
       ===================================================== */

    @Operation(
            summary = "Lista todos os itens (inclui rascunhos)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<ItemAcervo> listarTodos() {
        return service.listarTodos();
    }

    @Operation(
            summary = "Cria novo item de acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA', 'FOTOGRAFA')")
    public Mono<ItemAcervo> criar(@RequestBody ItemAcervoCreateDTO dto) {
        return service.criar(dto);
    }

    @Operation(
            summary = "Atualiza item (proteção por papel e propriedade)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA', 'FOTOGRAFA')")
    public Mono<ResponseEntity<ItemAcervo>> atualizar(
            @PathVariable String id,
            @RequestBody ItemAcervoCreateDTO dto,
            Authentication authentication
    ) {
        String identificador = authentication.getName();
        Set<String> roles = authentication.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        return service.atualizarProtegido(id, dto, identificador, roles)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Publica item no acervo",
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
            summary = "Remove item do acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> remover(@PathVariable String id) {
        return service.remover(id);
    }

    /* =====================================================
       GESTÃO DE ARQUIVOS
       ===================================================== */

    @Operation(
            summary = "Upload avulso para o Cloudinary",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA', 'FOTOGRAFA')")
    public Mono<ResponseEntity<FotoDTO>> uploadAvulso(@RequestPart("file") FilePart file) {
        return service.uploadCloudinaryPuro(file)
                .map(ResponseEntity::ok);
    }

    @Operation(
            summary = "Adiciona foto a um item do acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA', 'FOTOGRAFA')")
    public Mono<ResponseEntity<FotoDTO>> uploadFoto(
            @PathVariable String id,
            @RequestPart("file") FilePart file,
            @RequestPart("metadata") String metadataStr
    ) {
        return Mono.fromCallable(() -> objectMapper.readValue(metadataStr, FotoDTO.class))
                .flatMap(metadata -> service.adicionarFoto(id, file, metadata))
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Erro no upload de foto do item {}: {}", id, e.getMessage()))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }
}
