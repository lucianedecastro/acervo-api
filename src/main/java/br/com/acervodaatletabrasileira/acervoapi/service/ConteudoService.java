package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConteudoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConteudoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
public class ConteudoService {

    @Autowired
    private ConteudoRepository repository;


    @Autowired
    private FirestoreDirectService directService;

    // Os métodos de busca estão corretos
    public Mono<Conteudo> findById(String id) {
        return repository.findById(id);
    }

    public Flux<Conteudo> findAll() {
        return repository.findAll();
    }


    public Mono<Conteudo> save(ConteudoDTO dto) {
        Conteudo novoConteudo = new Conteudo();
        novoConteudo.setId(UUID.randomUUID().toString()); // Gera o ID
        novoConteudo.setSlug(dto.slug());
        novoConteudo.setTitulo(dto.titulo());
        novoConteudo.setConteudoHTML(dto.conteudoHTML());


        return directService.saveConteudo(novoConteudo);
    }

    public Mono<Conteudo> update(String id, ConteudoDTO dto) {
        return repository.findById(id)
                .flatMap(existingConteudo -> {
                    existingConteudo.setTitulo(dto.titulo());
                    existingConteudo.setSlug(dto.slug());
                    existingConteudo.setConteudoHTML(dto.conteudoHTML());


                    return directService.saveConteudo(existingConteudo);
                });
    }


    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }
}