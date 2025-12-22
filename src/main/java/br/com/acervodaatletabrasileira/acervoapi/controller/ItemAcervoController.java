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
        name = "Acervo",
        description = """
                Endpoints públicos e administrativos do Acervo Carmen Lydia
                da Mulher Brasileira no Esporte.

                - Leitura pública: apenas itens PUBLICADOS
                - Escrita e curadoria: acesso restrito a administradores
                """
)
public class ItemAcervoController {

    private final ItemAcervoService service;

    public ItemAcervoController(ItemAcervoService service) {
        this.service = service;
    }

    /* =====================================================
       CONSULTA PÚBLICA (APENAS ITENS PUBLICADOS)
       ===================================================== */

    @Operation(
            summary = "Lista todos os itens publicados do acervo",
            description = "Retorna apenas itens com status PUBLICADO, acessível ao público."
    )
    @GetMapping
    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return service.listarPublicados();
    }

    @Operation(
            summary = "Busca item publicado do acervo por ID",
            description = "Retorna um item específico do acervo, desde que esteja PUBLICADO."
    )
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ItemAcervoResponseDTO>> buscarPublicadoPorId(
            @PathVariable String id
    ) {
        return service.buscarPublicadoPorId(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Lista itens publicados do acervo por atleta",
            description = "Retorna todos os itens publicados associados a uma atleta."
    )
    @GetMapping("/atleta/{atletaId}")
    public Flux<ItemAcervoResponseDTO> listarPublicadosPorAtleta(
            @PathVariable String atletaId
    ) {
        return service.listarPublicadosPorAtleta(atletaId);
    }

    @Operation(
            summary = "Lista itens publicados do acervo por modalidade",
            description = "Retorna todos os itens publicados associados a uma modalidade esportiva."
    )
    @GetMapping("/modalidade/{modalidadeId}")
    public Flux<ItemAcervoResponseDTO> listarPublicadosPorModalidade(
            @PathVariable String modalidadeId
    ) {
        return service.listarPublicadosPorModalidade(modalidadeId);
    }

    /* =====================================================
       ADMIN / CURADORIA (JWT)
       ===================================================== */

    @Operation(
            summary = "Lista todos os itens do acervo (rascunho + publicado)",
            description = "Endpoint administrativo para curadoria do acervo.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    public Flux<ItemAcervo> listarTodos() {
        return service.listarTodos();
    }

    @Operation(
            summary = "Cria um novo item do acervo",
            description = "Cria um novo item do acervo com status inicial RASCUNHO.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ItemAcervo> criar(
            @RequestBody ItemAcervoCreateDTO dto
    ) {
        return service.criar(dto);
    }

    @Operation(
            summary = "Atualiza um item do acervo",
            description = "Atualiza os dados de um item existente do acervo.",
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
            summary = "Publica um item do acervo",
            description = "Altera o status do item para PUBLICADO, tornando-o visível ao público.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/publicar")
    public Mono<ResponseEntity<ItemAcervo>> publicar(
            @PathVariable String id
    ) {
        return service.publicar(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Remove um item do acervo",
            description = "Remove definitivamente um item do acervo.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> remover(
            @PathVariable String id
    ) {
        return service.remover(id);
    }

    /* =====================================================
       UPLOAD DE FOTO (ADMIN)
       ===================================================== */

    @Operation(
            summary = "Upload avulso de imagem",
            description = "Realiza o upload de uma imagem para o Cloudinary e retorna os dados da foto sem vincular a um item específico ainda.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/upload",
            consumes = "multipart/form-data"
    )
    public Mono<ResponseEntity<FotoDTO>> uploadAvulso(
            @RequestPart("file") FilePart file
    ) {
        // Assume que seu service tem um método para upload puro no Cloudinary
        return service.uploadCloudinaryPuro(file)
                .map(ResponseEntity::ok);
    }

    @Operation(
            summary = "Upload de foto para um item do acervo",
            description = """
                    Realiza o upload de uma imagem e associa ao item do acervo.

                    A requisição deve ser multipart/form-data contendo:
                    - file: arquivo de imagem
                    - metadata: JSON com metadados históricos da foto
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/{id}/fotos",
            consumes = "multipart/form-data"
    )
    public Mono<ResponseEntity<FotoDTO>> uploadFoto(
            @PathVariable String id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Arquivo de imagem",
                    required = true,
                    content = @Content(schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("file") FilePart file,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Metadados históricos da imagem",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FotoDTO.class))
            )
            @RequestPart("metadata") FotoDTO metadata
    ) {
        return service.adicionarFoto(id, file, metadata)
                .map(ResponseEntity::ok);
    }
}