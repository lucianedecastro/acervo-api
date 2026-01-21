package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FotografaRepository
        extends ReactiveMongoRepository<Fotografa, String> {

    /**
     * Busca fotógrafa pelo e-mail.
     * Essencial para login no Dashboard (/me).
     */
    Mono<Fotografa> findByEmail(String email);

    /**
     * Busca fotógrafa pelo slug único.
     * Usado no perfil público.
     */
    Mono<Fotografa> findBySlug(String slug);

    /**
     * Busca fotógrafas pelo nome (case-insensitive).
     * Útil para mecanismos de busca no acervo.
     */
    Flux<Fotografa> findByNomeContainingIgnoreCase(String nome);

    /**
     * Busca por CPF para evitar duplicidade
     * e para uso administrativo/fiscal.
     */
    Mono<Fotografa> findByCpf(String cpf);

    /**
     * Filtra fotógrafas por categoria (HISTORICA, ATIVA, ESPOLIO).
     */
    Flux<Fotografa> findByCategoria(Fotografa.CategoriaFotografa categoria);

    /**
     * Filtra fotógrafas por status operacional.
     */
    Flux<Fotografa> findByStatusFotografa(Fotografa.StatusFotografa statusFotografa);
}

