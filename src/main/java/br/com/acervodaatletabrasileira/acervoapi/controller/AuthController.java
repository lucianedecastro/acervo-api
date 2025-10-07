package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin; // NOVO: Importa o modelo de Admin
import br.com.acervodaatletabrasileira.acervoapi.service.AuthService;
import br.com.acervodaatletabrasileira.acervoapi.service.UsuarioAdminService; // NOVO: Serviço para salvar Admin
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; // NOVO: Criptografia
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
@Tag(name = "Autenticação", description = "Endpoint para autenticação de administradores")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioAdminService usuarioAdminService; // NOVO: Para salvar o primeiro usuário

    @Autowired
    private PasswordEncoder passwordEncoder; // NOVO: Para criptografar a senha

    @Operation(summary = "Realiza o login de um administrador e retorna um token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido, retorna o token",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
                    content = @Content)
    })
    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody AuthRequest authRequest) {
        return authService.authenticate(authRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(401).body(e.getMessage())));
    }

    // --- ROTA DE CADASTRO TEMPORÁRIA ---
    @Operation(summary = "TEMPORÁRIO: Cria o primeiro usuário administrador (deve ser removido após o uso)")
    @ApiResponse(responseCode = "201", description = "Administrador criado com sucesso")
    @PostMapping("/register_temp")
    public Mono<ResponseEntity<UsuarioAdmin>> registerAdminTemp(@RequestBody UsuarioAdmin admin) {
        // 1. Criptografa a senha antes de salvar no banco de dados (SEGURANÇA!)
        admin.setSenha(passwordEncoder.encode(admin.getSenha()));

        // 2. Salva o usuário no Firestore
        return usuarioAdminService.save(admin)
                .map(savedAdmin -> new ResponseEntity<>(savedAdmin, HttpStatus.CREATED));
    }
    // --- FIM DA ROTA TEMPORÁRIA ---
}