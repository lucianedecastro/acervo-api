package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            ReactiveAuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Autenticação Unificada:
     * O AuthenticationManager utiliza o seu UserDetailsServiceImpl para buscar
     * tanto na coleção de Admins quanto na de Atletas.
     */
    public Mono<String> authenticate(AuthRequest authRequest) {

        // Normalização defensiva do identificador
        String identifier = authRequest.email() != null
                ? authRequest.email().trim()
                : "";

        if (identifier.isBlank() || authRequest.senha() == null) {
            return Mono.error(new RuntimeException("Credenciais inválidas ou usuário não encontrado"));
        }

        return authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                identifier,
                                authRequest.senha()
                        )
                )
                .map(authentication -> {
                    // Se chegou aqui, as credenciais (e-mail e senha) já foram validadas!
                    // O objeto 'authentication' agora contém o Principal (Admin ou Atleta) e a Role.
                    return jwtService.generateToken(authentication);
                })
                .onErrorResume(e ->
                        Mono.error(new RuntimeException("Credenciais inválidas ou usuário não encontrado"))
                );
    }
}
