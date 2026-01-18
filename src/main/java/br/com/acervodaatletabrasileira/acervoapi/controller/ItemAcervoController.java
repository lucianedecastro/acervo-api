package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoCreateDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.service.ItemAcervoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/acervo")
@Tag(
        name = "Acervo Digital Carmen Lydia",
        description = """
                Infraestrutura de preservação e circulação de acervos digitais.
                
                - Leitura pública: Itens catalogados de acervos pessoais de atletas.
                - Licenciamento: Acesso a materiais para download e uso remunerado.
                - Curadoria: Gestão administrativa de direitos e procedência.
                """
)
public class ItemAcervoController {

    private final ItemAcervoService service;

    public ItemAcervoController(ItemAcervoService service) {
        this.service = service;
    }

    /* =====================================================
       CONSULTA PÚBLICA (VALORIZANDO A MEMÓRIA ATLETA)
       ===================================================== */

    @Operation(summary = "Lista itens publicados do acervo")
    @GetMapping
    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return service.listarPublicados();
    }

    @Operation(summary = "Busca detalhe de um item do acervo")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ItemAcervoResponseDTO>> buscarPorId(@PathVariable String id) {
        return service.buscarPublicadoPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Filtra itens do acervo pessoal por atleta")
    @GetMapping("/atleta/{atletaId}")
    public Flux<ItemAcervoResponseDTO> listarPorAtleta(@PathVariable String atletaId) {
        return service.listarPublicadosPorAtleta(atletaId);
    }

    /* =====================================================
       ADMIN / CURADORIA (CONTROLE DE ACERVO E LICENCIAMENTO)
       ===================================================== */

    @Operation(
            summary = "Lista base total para curadoria",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    public Flux<ItemAcervo> listarTodos() {
        return service.listarTodos();
    }

    @Operation(
            summary = "Registra novo item no acervo (Procedência e Preço)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ItemAcervo> criar(@RequestBody ItemAcervoCreateDTO dto) {
        return service.criar(dto);
    }

    @Operation(
            summary = "Atualiza metadados históricos e regras de licenciamento",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ItemAcervo>> atualizar(
            @PathVariable String id,
            @RequestBody ItemAcervoCreateDTO dto
    ) {
        return service.atualizar(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Aprova item para circulação pública/remunerada",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/publicar")
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
    public Mono<Void> remover(@PathVariable String id) {
        return service.remover(id);
    }

    /* =====================================================
       GESTÃO DE ARQUIVOS DIGITAIS
       ===================================================== */

    @Operation(
            summary = "Upload de material bruto (Cloudinary)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ResponseEntity<FotoDTO>> uploadAvulso(@RequestPart("file") FilePart file) {
        return service.uploadCloudinaryPuro(file).map(ResponseEntity::ok);
    }

    @Operation(
            summary = "Vincula arquivo digital ao item do acervo com metadados",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/{id}/fotos", consumes = "multipart/form-data")
    public Mono<ResponseEntity<FotoDTO>> uploadFoto(
            @PathVariable String id,
            @RequestPart("file") FilePart file,
            @RequestPart("metadata") FotoDTO metadata
    ) {
        return service.adicionarFoto(id, file, metadata).map(ResponseEntity::ok);
    }
}