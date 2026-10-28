package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.EsqueciSenhaRequest;
import br.com.acervodaatletabrasileira.acervoapi.dto.RedefinirSenhaRequest;
import br.com.acervodaatletabrasileira.acervoapi.service.RecuperacaoSenhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService service;

    public RecuperacaoSenhaController(RecuperacaoSenhaService service) {
        this.service = service;
    }

    @Operation(summary = "Pede o e-mail de redefinição de senha (resposta igual exista ou não a conta)")
    @PostMapping("/esqueci-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Map<String, String>> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        return service.solicitar(request.email())
                .thenReturn(Map.of("mensagem",
                        "Se o e-mail estiver cadastrado, enviaremos as instruções para redefinir a senha."));
    }

    @Operation(summary = "Define a nova senha a partir do token recebido por e-mail")
    @PostMapping("/redefinir-senha")
    public Mono<Map<String, String>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        return service.redefinir(request.token(), request.novaSenha())
                .thenReturn(Map.of("mensagem", "Senha redefinida com sucesso. Faça login com a nova senha."));
    }
}
