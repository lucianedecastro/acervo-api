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
                
                Diretriz Institucional:
                - Blockchain é utilizada exclusivamente como camada de governança
                - Registro imutável NÃO ocorre automaticamente na publicação
                - O carimbo institucional é um ato administrativo separado
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
            summary = "Lista todos os itens (Dashboard Admin)",
            description = "Retorna os itens visíveis. Endpoint administrativo temporário.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<ItemAcervoResponseDTO> listarAdmin() {
        return service.listarPublicados();
    }

    @Operation(
            summary = "Cria novo item de acervo",
            description = "Cria o registro inicial. O registro em Blockchain é um ato institucional separado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA', 'FOTOGRAFA')")
    public Mono<ItemAcervo> criar(@RequestBody ItemAcervoCreateDTO dto) {
        return service.criar(dto);
    }

    /**
     * Publica item no acervo.
     *
     * IMPORTANTE:
     * A publicação torna o item visível.
     * O registro imutável na Blockchain é um ato institucional
     * separado, realizado via endpoint específico de governança.
     */
    @Operation(
            summary = "Publica item no acervo",
            description = "Torna o item visível. Não realiza registro automático na Blockchain.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ItemAcervo>> publicar(@PathVariable String id) {
        return service.publicar(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint específico de governança citado no comentário acima:
     * registra o hash de integridade (gerado no upload da foto)
     * na Blockchain. Ato administrativo, deliberadamente manual.
     */
    @Operation(
            summary = "Registra o selo institucional (Blockchain) do item",
            description = "Grava na Blockchain o hash de integridade já calculado no upload da foto principal.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/registrar-blockchain")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ItemAcervo>> registrarSeloBlockchain(
            @PathVariable String id,
            Authentication authentication
    ) {
        return service.registrarSeloBlockchain(id, authentication.getName())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       GESTÃO DE ARQUIVOS (COM HASHING AUTOMÁTICO)
       ===================================================== */

    @Operation(
            summary = "Adiciona foto a um item e gera Hash de Integridade",
            description = "Calcula o SHA-256 do arquivo antes do upload.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA', 'FOTOGRAFA')")
    public Mono<ResponseEntity<FotoDTO>> uploadFoto(
            @PathVariable String id,
            @RequestPart("file") FilePart file,
            @RequestPart("metadata") Mono<String> metadataStr
    ) {
        return metadataStr
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, FotoDTO.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Erro ao desserializar metadata", e);
                    }
                })
                .flatMap(metadata -> service.adicionarFoto(id, file, metadata))
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Erro no upload de foto do item {}:", id, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
}