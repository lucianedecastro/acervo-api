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
@RequestMapping("/admin") // Mantido o prefixo para consistência com o SecurityConfig
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
    // REGISTRO TEMPORÁRIO (Escondido do Swagger via @Hidden)
    // =====================================================
    @Hidden
    @PostMapping("/register-temp")
    @Operation(summary = "Cadastra um administrador temporário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Admin cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro no cadastro")
    })
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
        return authService.authenticate(authRequest)
                .map(token -> ResponseEntity.ok(Map.of(
                        "token", token,
                        "type", "Bearer",
                        "expiresIn", "86400"
                )))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Credenciais inválidas ou usuário não encontrado"))));
    }
}