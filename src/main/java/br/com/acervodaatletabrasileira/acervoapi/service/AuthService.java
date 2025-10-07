// src/main/java/br/com/acervodaatletabrasileira/acervoapi/service/AuthService.java
package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    @Autowired
    private UsuarioAdminRepository usuarioAdminRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    public Mono<String> authenticate(AuthRequest authRequest) {
        return usuarioAdminRepository.findByEmail(authRequest.getEmail())
                .flatMap(usuario -> {
                    // Compara a senha enviada na requisição com a senha codificada no banco
                    if (passwordEncoder.matches(authRequest.getSenha(), usuario.getPassword())) {
                        // Se as senhas baterem, gera o token
                        return Mono.just(jwtService.generateToken(usuario));
                    } else {
                        // Se não baterem, retorna um erro
                        return Mono.error(new RuntimeException("Credenciais inválidas"));
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Usuário não encontrado"))); // Caso o email não exista
    }
}
