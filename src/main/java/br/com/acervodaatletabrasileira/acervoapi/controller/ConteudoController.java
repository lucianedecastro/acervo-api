package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConteudoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import br.com.acervodaatletabrasileira.acervoapi.service.ConteudoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

    @Autowired
    private ConteudoService conteudoService;

    @GetMapping
    public Flux<Conteudo> getAllConteudos() {
        return conteudoService.findAll();
    }

    // ✅ CORREÇÃO: A URL agora usa {id}.
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Conteudo>> getConteudoById(@PathVariable String id) {
        return conteudoService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Conteudo> createConteudo(@RequestBody ConteudoDTO conteudoDTO) {
        return conteudoService.save(conteudoDTO);
    }

    // ✅ CORREÇÃO: A URL de update também usa {id}.
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Conteudo>> updateConteudo(
            @PathVariable String id,
            @RequestBody ConteudoDTO conteudoDTO) {

        return conteudoService.update(id, conteudoDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // ✅ CORREÇÃO: A URL de delete também usa {id}.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteConteudo(@PathVariable String id) {
        return conteudoService.deleteById(id);
    }
}