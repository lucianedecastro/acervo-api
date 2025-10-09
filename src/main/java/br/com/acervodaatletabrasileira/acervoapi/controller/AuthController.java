package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AuthRequest;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.service.AuthService;
import br.com.acervodaatletabrasileira.acervoapi.service.UsuarioAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Date;

@RestController
@RequestMapping("/admin")
@Tag(name = "Autenticação", description = "Endpoint para autenticação de administradores")
public class AuthController {

    @Autowired
    private UsuarioAdminService usuarioAdminService;

    @Autowired
    private AuthService authService;

    @Operation(summary = "Cadastra um administrador temporário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro no cadastro")
    })
    @PostMapping("/register_temp")
    public Mono<ResponseEntity<String>> registerAdminTemp(@RequestBody AuthRequest authRequest) {
        UsuarioAdmin admin = new UsuarioAdmin();
        admin.setEmail(authRequest.getEmail());
        admin.setSenha(authRequest.getSenha());
        admin.setRole("ADMIN");
        admin.setCriadoEm(new Date());

        return usuarioAdminService.save(admin)
                .map(savedAdmin -> ResponseEntity.ok("Admin cadastrado com sucesso: " + savedAdmin.getEmail()))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.badRequest().body("Erro: " + e.getMessage()));
                });
    }

    @Operation(summary = "Realiza login do administrador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody AuthRequest authRequest) {
        return authService.authenticate(authRequest)
                .map(token -> ResponseEntity.ok("{\"token\": \"" + token + "\"}"))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(401).body("Credenciais inválidas")));
    }
}