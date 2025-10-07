// src/main/java/br/com/acervodaatletabrasileira/acervoapi/service/UserDetailsServiceImpl.java
package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    @Autowired
    private UsuarioAdminRepository repository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        // Busca o usuário pelo email e faz um cast para UserDetails
        return repository.findByEmail(username)
                .cast(UserDetails.class);
    }
}
