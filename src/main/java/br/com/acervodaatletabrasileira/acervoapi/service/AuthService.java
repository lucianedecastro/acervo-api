package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    @Autowired
    private UsuarioAdminService usuarioAdminService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public Mono<String> authenticate(AuthRequest authRequest) {
        return usuarioAdminService.findByEmail(authRequest.getEmail())
                .flatMap(admin -> {
                    if (passwordEncoder.matches(authRequest.getSenha(), admin.getSenha())) {
                        // ✅ CORREÇÃO: generateToken espera um UsuarioAdmin, não String
                        String token = jwtService.generateToken(admin);
                        return Mono.just(token);
                    } else {
                        return Mono.error(new RuntimeException("Credenciais inválidas"));
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Usuário não encontrado")));
    }
}