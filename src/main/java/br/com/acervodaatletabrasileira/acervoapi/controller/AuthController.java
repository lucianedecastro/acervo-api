package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.service.AuthService;
import br.com.acervodaatletabrasileira.acervoapi.service.UsuarioAdminService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth") // Normalizado: agora é a porta de entrada para todos
@Tag(name = "Autenticação", description = "Endpoints de acesso ao ecossistema (Admin e Atletas)")
public class AuthController {

    private final UsuarioAdminService usuarioAdminService;
    private final AuthService authService;

    public AuthController(
            UsuarioAdminService usuarioAdminService,
            AuthService authService
    ) {
        this.usuarioAdminService = usuarioAdminService;
        this.authService = authService;
    }

    // =====================================================
    // LOGIN UNIFICADO (Híbrido: Admin e Atletas)
    // =====================================================
    @PostMapping("/login")
    @Operation(summary = "Realiza login unificado", description = "Aceita credenciais de Administradores e Atletas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso (Retorna JWT)"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou usuário não encontrado")
    })
    public Mono<ResponseEntity<Map<String, String>>> login(
            @RequestBody AuthRequest authRequest
    ) {
        // Normalização defensiva do identificador (evita email vazio ou com espaços)
        String identifier = authRequest.email() != null
                ? authRequest.email().trim()
                : "";

        if (identifier.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciais inválidas ou usuário não encontrado")));
        }

        // O authService chama o ReactiveAuthenticationManager,
        // que usa o UserDetailsServiceImpl (que busca nas duas coleções).
        return authService.authenticate(
                        new AuthRequest(identifier, authRequest.senha())
                )
                .map(token -> ResponseEntity.ok(Map.of(
                        "token", token,
                        "type", "Bearer",
                        "expiresIn", "86400"
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciais inválidas ou usuário não encontrado"))));
    }

    // =====================================================
    // REGISTRO TEMPORÁRIO (Apenas para Setup inicial)
    // =====================================================
    @Hidden
    @PostMapping("/register-admin")
    public Mono<ResponseEntity<Map<String, String>>> registerAdminTemp(
            @RequestBody AuthRequest authRequest
    ) {
        UsuarioAdmin admin = new UsuarioAdmin();
        admin.setEmail(authRequest.email());
        admin.setSenha(authRequest.senha());
        admin.setRole("ROLE_ADMIN");
        admin.setCriadoEm(Instant.now());

        return usuarioAdminService.save(admin)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Admin cadastrado com sucesso", "email", saved.getEmail())))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(Map.of("error", "Erro ao cadastrar: " + e.getMessage()))));
    }
}
