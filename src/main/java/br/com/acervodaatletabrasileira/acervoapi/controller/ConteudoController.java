package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConteudoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import br.com.acervodaatletabrasileira.acervoapi.service.ConteudoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/conteudos")
@Tag(name = "Conteúdos", description = "Endpoints para gerenciamento de conteúdo textual")
public class ConteudoController {

    @Autowired
    private ConteudoService conteudoService;

    // --- READ ---
    @Operation(summary = "Lista todos os blocos de conteúdo editáveis")
    @GetMapping
    public Flux<Conteudo> getAllConteudos() {
        return conteudoService.findAll();
    }

    @Operation(summary = "Busca um bloco de conteúdo pelo seu slug (ID)")
    @GetMapping("/{slug}")
    public Mono<ResponseEntity<Conteudo>> getConteudoBySlug(@PathVariable String slug) {
        return conteudoService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- UPDATE ---
    @Operation(summary = "Atualiza um bloco de conteúdo (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{slug}")
    public Mono<ResponseEntity<Conteudo>> updateConteudo(
            @PathVariable String slug,
            @RequestBody ConteudoDTO dto) {

        // Busca o conteúdo existente pelo slug
        return conteudoService.findBySlug(slug)
                .flatMap(existingConteudo -> {
                    // Atualiza os campos com os dados do DTO
                    existingConteudo.setTitulo(dto.titulo());
                    existingConteudo.setConteudoHTML(dto.conteudoHTML());

                    // Salva o conteúdo atualizado
                    return conteudoService.save(existingConteudo);
                })
                .map(ResponseEntity::ok) // Se tudo deu certo, retorna 200 OK com o objeto salvo
                .defaultIfEmpty(ResponseEntity.notFound().build()); // Se não encontrou pelo slug, retorna 404
    }
}
