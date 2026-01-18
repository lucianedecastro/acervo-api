package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UsuarioAdminRepository
        extends ReactiveMongoRepository<UsuarioAdmin, String> {

    Mono<UsuarioAdmin> findByEmail(String email);
}
