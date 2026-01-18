package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Lista todas as atletas do acervo")
    @GetMapping
    public Flux<Atleta> listarTodas() {
        return atletaService.findAll();
    }

    @Operation(summary = "Busca uma atleta pelo ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Atleta>> buscarPorId(@PathVariable String id) {
        return atletaService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       ADMIN – ESCRITA (JWT)
       ===================================================== */

    @Operation(
            summary = "Cadastra uma nova atleta",
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
            summary = "Remove uma atleta do acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> remover(@PathVariable("id") String id) {
        return atletaService.deleteById(id);
    }
}
