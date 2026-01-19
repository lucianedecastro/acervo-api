package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AtletaRepository
        extends ReactiveMongoRepository<Atleta, String> {

    /**
     * Busca uma atleta pelo e-mail.
     * Essencial para o processo de login no Dashboard da Atleta e endpoint /me.
     */
    Mono<Atleta> findByEmail(String email);

    /**
     * Busca uma atleta pelo slug único.
     * Usado para carregar o perfil público (ex: acervoatleta.com.br/atleta/maria-lenk).
     */
    Mono<Atleta> findBySlug(String slug);

    /**
     * Busca atletas pelo nome (case-insensitive).
     * Útil para mecanismos de busca no site.
     */
    Flux<Atleta> findByNomeContainingIgnoreCase(String nome);

    /**
     * Busca por CPF para evitar duplicidade no cadastro
     * e para buscas na área administrativa de pagamentos.
     */
    Mono<Atleta> findByCpf(String cpf);

    /**
     * Filtra atletas por categoria (HISTORICA, ATIVA, ESPOLIO).
     */
    Flux<Atleta> findByCategoria(Atleta.CategoriaAtleta categoria);
}