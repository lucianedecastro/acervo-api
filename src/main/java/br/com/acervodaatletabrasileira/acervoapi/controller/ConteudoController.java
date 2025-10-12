package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConteudoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import br.com.acervodaatletabrasileira.acervoapi.service.ConteudoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // Assumindo que você usará
import io.swagger.v3.oas.annotations.tags.Tag; // Assumindo que você usará
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/conteudos")
@Tag(name = "Conteúdos") // Adicionei para consistência
public class ConteudoController {

    @Autowired
    private ConteudoService conteudoService;

    @Operation(summary = "Lista todas as páginas de conteúdo")
    @GetMapping
    public Flux<Conteudo> getAllConteudos() {
        return conteudoService.findAll();
    }

    // ✅ CORREÇÃO: A URL agora usa {slug} para ser mais clara e corresponder ao frontend.
    @Operation(summary = "Busca uma página de conteúdo pelo seu slug")
    @GetMapping("/{slug}")
    public Mono<ResponseEntity<Conteudo>> getConteudoBySlug(@PathVariable String slug) {
        return conteudoService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // ✅ CORREÇÃO: O método de criação agora usa o DTO e chama o service correto.
    @Operation(summary = "Cria uma nova página de conteúdo", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Conteudo> createConteudo(@RequestBody ConteudoDTO conteudoDTO) {
        return conteudoService.save(conteudoDTO);
    }

    // ✅ CORREÇÃO: A URL de atualização também usa {slug}.
    @Operation(summary = "Atualiza uma página de conteúdo", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{slug}")
    public Mono<ResponseEntity<Conteudo>> updateConteudo(
            @PathVariable String slug,
            @RequestBody ConteudoDTO conteudoDTO) {

        return conteudoService.update(slug, conteudoDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // ✅ CORREÇÃO: A URL de deleção também usa {slug}.
    @Operation(summary = "Deleta uma página de conteúdo", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteConteudo(@PathVariable String slug) {
        return conteudoService.deleteBySlug(slug);
    }
}