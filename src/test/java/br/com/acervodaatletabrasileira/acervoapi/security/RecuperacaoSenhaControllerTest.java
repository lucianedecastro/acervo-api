package br.com.acervodaatletabrasileira.acervoapi.security;

import br.com.acervodaatletabrasileira.acervoapi.config.GlobalExceptionHandler;
import br.com.acervodaatletabrasileira.acervoapi.config.JwtAuthFilter;
import br.com.acervodaatletabrasileira.acervoapi.config.SecurityConfig;
import br.com.acervodaatletabrasileira.acervoapi.controller.RecuperacaoSenhaController;
import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import br.com.acervodaatletabrasileira.acervoapi.service.RecuperacaoSenhaService;
import br.com.acervodaatletabrasileira.acervoapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = RecuperacaoSenhaController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class RecuperacaoSenhaControllerTest {

    @Autowired WebTestClient client;

    @MockBean UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean JwtService jwtService;
    @MockBean RecuperacaoSenhaService service;

    @Test
    void esqueciSenha_anonimoRecebe202ComMensagemNeutra() {
        when(service.solicitar(any())).thenReturn(Mono.empty());

        client.post().uri("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"qualquer@teste.com\"}")
                .exchange().expectStatus().isAccepted()
                .expectBody().jsonPath("$.mensagem").value(m -> org.assertj.core.api.Assertions
                        .assertThat(m.toString()).startsWith("Se o e-mail estiver cadastrado"));
        verify(service).solicitar("qualquer@teste.com");
    }

    @Test
    void esqueciSenha_emailInvalidoRecebe400() {
        client.post().uri("/auth/esqueci-senha").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"nao-e-email\"}")
                .exchange().expectStatus().isBadRequest();
        verifyNoInteractions(service);
    }

    @Test
    void redefinir_senhaCurtaRecebe400() {
        client.post().uri("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"token\":\"abc\",\"novaSenha\":\"123\"}")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.campos.novaSenha").exists();
        verifyNoInteractions(service);
    }

    @Test
    void redefinir_linkInvalidoRecebe400ComMensagem() {
        when(service.redefinir(any(), any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Link inválido ou expirado.")));

        client.post().uri("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"token\":\"abc\",\"novaSenha\":\"senha-nova-123\"}")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("Link inválido ou expirado.");
    }

    @Test
    void redefinir_sucessoRecebe200() {
        when(service.redefinir(any(), any())).thenReturn(Mono.empty());

        client.post().uri("/auth/redefinir-senha").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"token\":\"abc\",\"novaSenha\":\"senha-nova-123\"}")
                .exchange().expectStatus().isOk();
    }
}
