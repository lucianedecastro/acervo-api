package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConteudoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConteudoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID; // ✅ Importar para gerar o ID

@Service
public class ConteudoService {

    @Autowired
    private ConteudoRepository repository;

    // Os métodos de busca agora usam 'id'
    public Mono<Conteudo> findById(String id) {
        return repository.findById(id);
    }

    public Flux<Conteudo> findAll() {
        return repository.findAll();
    }

    // ✅ CORREÇÃO: O save agora gera um UUID para o campo 'id'
    public Mono<Conteudo> save(ConteudoDTO dto) {
        Conteudo novoConteudo = new Conteudo();
        novoConteudo.setId(UUID.randomUUID().toString()); // Gera o ID
        novoConteudo.setSlug(dto.slug());
        novoConteudo.setTitulo(dto.titulo());
        novoConteudo.setConteudoHTML(dto.conteudoHTML());
        return repository.save(novoConteudo);
    }

    // ✅ CORREÇÃO: O update usa o 'id' para encontrar e salvar.
    public Mono<Conteudo> update(String id, ConteudoDTO dto) {
        return repository.findById(id)
                .flatMap(existingConteudo -> {
                    existingConteudo.setTitulo(dto.titulo());
                    existingConteudo.setSlug(dto.slug()); // Slug agora pode ser editado
                    existingConteudo.setConteudoHTML(dto.conteudoHTML());
                    return repository.save(existingConteudo);
                });
    }

    // ✅ CORREÇÃO: O delete usa 'id'.
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }
}