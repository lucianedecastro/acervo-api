package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPerfilDTO; // Importado
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/atletas")
@Tag(name = "Atletas", description = "Endpoints públicos e administrativos do acervo de atletas")
public class AtletaController {

    private final AtletaService atletaService;

    public AtletaController(AtletaService atletaService) {
        this.atletaService = atletaService;
    }

    /* =====================================================
       LEITURA PÚBLICA
       ===================================================== */

    @Operation(summary = "Lista atletas do acervo (opcionalmente filtradas por categoria)")
    @GetMapping
    public Flux<Atleta> listar(
            @Parameter(description = "Filtrar por: HISTORICA, ATIVA ou ESPOLIO")
            @RequestParam(required = false) Atleta.CategoriaAtleta categoria
    ) {
        if (categoria != null) {
            return atletaService.findAll()
                    .filter(atleta -> atleta.getCategoria() == categoria);
        }
        return atletaService.findAll();
    }

    @Operation(summary = "Busca uma atleta pelo ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Atleta>> buscarPorId(@PathVariable String id) {
        return atletaService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Ajustado para retornar o Perfil Completo (Atleta + Itens do Acervo)
     */
    @Operation(summary = "Busca o perfil completo da atleta pelo Slug (Dados + Acervo)")
    @GetMapping("/perfil/{slug}")
    public Mono<ResponseEntity<AtletaPerfilDTO>> buscarPorSlug(@PathVariable String slug) {
        return atletaService.getPerfilCompletoBySlug(slug) // Chama o novo método do Service
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       ADMIN – ESCRITA E VERIFICAÇÃO (JWT Requerido)
       ===================================================== */

    @Operation(
            summary = "Cadastra uma nova atleta (Histórica, Ativa ou Espólio)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atleta> criar(@RequestBody AtletaFormDTO dto) {
        return atletaService.create(dto);
    }

    @Operation(
            summary = "Atualiza uma atleta existente",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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

    @Operation(
            summary = "Remove uma atleta do acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> remover(@PathVariable String id) {
        return atletaService.deleteById(id)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
                .onErrorResume(e -> {
                    return Mono.just(new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED));
                });
    }
}