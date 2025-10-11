package br.com.acervodaatletabrasileira.acervoapi.service;

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

    @Autowired
    private FirestoreDirectService directService;

    // --- MÉTODOS DE LEITURA (Gasolina ⛽) ---
    public Flux<Conteudo> findAll() {
        return repository.findAll();
    }

    public Mono<Conteudo> findBySlug(String slug) {
        return repository.findById(slug);
    }

    // --- MÉTODO DE ESCRITA (Álcool 🌽) ---
    public Mono<Conteudo> save(Conteudo conteudo) {
        // O slug (ID) e o título vêm do DTO no Controller
        return directService.saveConteudo(conteudo);
    }
}
