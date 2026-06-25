package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AtletaRepository atletaRepository; // Injeção para checagem de Governança

    public AuthService(
            ReactiveAuthenticationManager authenticationManager,
            JwtService jwtService,
            AtletaRepository atletaRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.atletaRepository = atletaRepository;
    }

    /**
     * Autenticação Unificada com Trava de Governança:
     * Além de validar e-mail/senha, verifica o status de conformidade da atleta.
     */
    public Mono<String> authenticate(AuthRequest authRequest) {

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
                .flatMap(this::validarStatusGovernança) // Incremento: Valida se o perfil está apto
                .map(jwtService::generateToken)
                .onErrorResume(e ->
                        Mono.error(new RuntimeException(e.getMessage().contains("bloqueado") ? e.getMessage() : "Credenciais inválidas ou usuário não encontrado"))
                );
    }

    /**
     * Regra de Negócio: Impede login de usuários com pendências críticas ou rejeitados.
     */
    private Mono<Authentication> validarStatusGovernança(Authentication auth) {
        String email = auth.getName();

        // Buscamos se o usuário é uma Atleta (Admins passam direto)
        return atletaRepository.findByEmail(email)
                .flatMap(atleta -> {
                    // Se a atleta foi REJEITADA pela curadoria, bloqueamos o acesso ao Dashboard
                    if (atleta.getStatusVerificacao() == Atleta.StatusVerificacao.REJEITADO) {
                        return Mono.error(new RuntimeException("Acesso bloqueado: Perfil rejeitado na análise documental."));
                    }

                    // Se estiver PENDENTE, ela pode logar, mas o Token levará essa info (via JwtService)
                    // para o Frontend mostrar o aviso de "Aguardando Verificação".
                    return Mono.just(auth);
                })
                .defaultIfEmpty(auth); // Se não for atleta (for Admin), segue o fluxo normal
    }
}