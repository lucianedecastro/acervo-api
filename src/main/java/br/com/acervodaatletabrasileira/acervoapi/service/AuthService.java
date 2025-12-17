package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final UsuarioAdminService usuarioAdminService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioAdminService usuarioAdminService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioAdminService = usuarioAdminService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ==========================
    // AUTENTICAÇÃO ADMIN
    // ==========================
    public Mono<String> authenticate(AuthRequest authRequest) {

        return usuarioAdminService.findByEmail(authRequest.email()) // ✅ record access
                .switchIfEmpty(
                        Mono.error(new RuntimeException("Usuário não encontrado"))
                )
                .flatMap(admin -> validatePassword(authRequest, admin))
                .map(jwtService::generateToken);
    }

    private Mono<UsuarioAdmin> validatePassword(
            AuthRequest authRequest,
            UsuarioAdmin admin
    ) {
        if (!passwordEncoder.matches(authRequest.senha(), admin.getSenha())) { // ✅ record access
            return Mono.error(new RuntimeException("Credenciais inválidas"));
        }
        return Mono.just(admin);
    }
}
