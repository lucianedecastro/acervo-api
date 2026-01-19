package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadePublicaDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.service.ModalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/modalidades")
@Tag(name = "Modalidades", description = "Endpoints públicos e administrativos das modalidades do acervo")
public class ModalidadeController {

    private final ModalidadeService modalidadeService;

    public ModalidadeController(ModalidadeService modalidadeService) {
        this.modalidadeService = modalidadeService;
    }

    /* =====================================================
       LEITURA PÚBLICA (Com Proteção DTO e Filtro de Ativas)
       ===================================================== */

    @Operation(summary = "Lista todas as modalidades ativas para o público")
    @GetMapping
    public Flux<ModalidadePublicaDTO> listarTodas() {
        return modalidadeService.findAll()
                .filter(m -> Boolean.TRUE.equals(m.getAtiva()))
                .map(ModalidadePublicaDTO::fromModel);
    }

    @Operation(summary = "Busca uma modalidade ativa pelo ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ModalidadePublicaDTO>> buscarPorId(@PathVariable String id) {
        return modalidadeService.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getAtiva()))
                .map(m -> ResponseEntity.ok(ModalidadePublicaDTO.fromModel(m)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Busca uma modalidade ativa pelo Slug (URL Amigável)")
    @GetMapping("/slug/{slug}")
    public Mono<ResponseEntity<ModalidadePublicaDTO>> buscarPorSlug(@PathVariable String slug) {
        return modalidadeService.findBySlug(slug)
                .filter(m -> Boolean.TRUE.equals(m.getAtiva()))
                .map(m -> ResponseEntity.ok(ModalidadePublicaDTO.fromModel(m)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       ADMIN – GESTÃO E ESCRITA (JWT Requerido)
       ===================================================== */

    @Operation(
            summary = "Lista todas as modalidades (ativas e inativas) para gestão do Admin",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    public Flux<Modalidade> listarParaAdmin() {
        return modalidadeService.findAll();
    }

    @Operation(
            summary = "Cria uma nova modalidade",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Modalidade> criar(@RequestBody ModalidadeDTO dto) {
        return modalidadeService.create(dto);
    }

    @Operation(
            summary = "Atualiza uma modalidade existente",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Modalidade>> atualizar(
            @PathVariable String id,
            @RequestBody ModalidadeDTO dto
    ) {
        return modalidadeService.update(id, dto)
                .map(ResponseEntity::ok)
                .onErrorResume(
                        IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
                );
    }

    @Operation(
            summary = "Remove uma modalidade",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> remover(@PathVariable String id) {
        return modalidadeService.deleteById(id);
    }
}