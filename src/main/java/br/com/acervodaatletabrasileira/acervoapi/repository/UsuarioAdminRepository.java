package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository; // CORRIGIDO!
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UsuarioAdminRepository extends FirestoreReactiveRepository<UsuarioAdmin> {
    Mono<UsuarioAdmin> findByEmail(String email);
}
