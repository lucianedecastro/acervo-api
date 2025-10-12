package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConteudoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConteudoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ConteudoService {

    @Autowired
    private ConteudoRepository repository;

    // ✅ CORREÇÃO: Renomeado para clareza, mas chama o método padrão que agora busca pelo slug.
    public Mono<Conteudo> findBySlug(String slug) {
        return repository.findById(slug);
    }

    public Flux<Conteudo> findAll() {
        return repository.findAll();
    }

    // ✅ CORREÇÃO: Cria um novo documento usando o slug do DTO como ID.
    public Mono<Conteudo> save(ConteudoDTO dto) {
        Conteudo novoConteudo = new Conteudo();
        novoConteudo.setSlug(dto.slug()); // O ID do documento será este slug
        novoConteudo.setTitulo(dto.titulo());
        novoConteudo.setConteudoHTML(dto.conteudoHTML());
        return repository.save(novoConteudo);
    }

    // ✅ CORREÇÃO: Usa o slug para encontrar o documento e então o atualiza.
    public Mono<Conteudo> update(String slug, ConteudoDTO dto) {
        return repository.findById(slug)
                .flatMap(existingConteudo -> {
                    existingConteudo.setTitulo(dto.titulo());
                    existingConteudo.setConteudoHTML(dto.conteudoHTML());
                    // O slug (ID) não deve ser alterado durante uma edição
                    return repository.save(existingConteudo);
                });
    }

    // ✅ CORREÇÃO: Deleta o documento usando o slug como ID.
    public Mono<Void> deleteBySlug(String slug) {
        return repository.deleteById(slug);
    }
}