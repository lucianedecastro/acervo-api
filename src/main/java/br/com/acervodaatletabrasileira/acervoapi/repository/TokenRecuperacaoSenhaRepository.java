package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.TokenRecuperacaoSenha;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface TokenRecuperacaoSenhaRepository extends ReactiveMongoRepository<TokenRecuperacaoSenha, String> {

    Mono<TokenRecuperacaoSenha> findByTokenHash(String tokenHash);

    Mono<TokenRecuperacaoSenha> findFirstByEmailOrderByCriadoEmDesc(String email);

    Mono<Void> deleteByEmail(String email);

    Mono<Void> deleteByExpiraEmBefore(Instant limite);
}
