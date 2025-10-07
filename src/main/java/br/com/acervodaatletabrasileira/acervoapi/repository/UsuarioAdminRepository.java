// src/main/java/br/com/acervodaatletabrasileira/acervoapi/repository/UsuarioAdminRepository.java
package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UsuarioAdminRepository extends FirestoreReactiveRepository<UsuarioAdmin> {
    // Método customizado para buscar um usuário pelo seu email
    Mono<UsuarioAdmin> findByEmail(String email);
}
