package br.com.acervodaatletabrasileira.acervoapi.security;

import br.com.acervodaatletabrasileira.acervoapi.config.JwtAuthFilter;
import br.com.acervodaatletabrasileira.acervoapi.config.SecurityConfig;
import br.com.acervodaatletabrasileira.acervoapi.controller.AsaasWebhookController;
import br.com.acervodaatletabrasileira.acervoapi.controller.AuthController;
import br.com.acervodaatletabrasileira.acervoapi.controller.ModalidadeController;
import br.com.acervodaatletabrasileira.acervoapi.service.AsaasWebhookService;
import br.com.acervodaatletabrasileira.acervoapi.service.AuthService;
import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import br.com.acervodaatletabrasileira.acervoapi.service.ModalidadeService;
import br.com.acervodaatletabrasileira.acervoapi.service.UserDetailsServiceImpl;
import br.com.acervodaatletabrasileira.acervoapi.service.UsuarioAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = {AuthController.class, ModalidadeController.class, AsaasWebhookController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
@TestPropertySource(properties = {
        "app.admin.register-enabled=false",
        "asaas.webhook-token=segredo-de-teste"
})
class SecurityRulesTest {

    @Autowired WebTestClient client;

    @MockBean UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean JwtService jwtService;
    @MockBean AuthService authService;
    @MockBean UsuarioAdminService usuarioAdminService;
    @MockBean ModalidadeService modalidadeService;
    @MockBean AsaasWebhookService webhookService;

    private static final String ADMIN_BODY = "{\"email\":\"x@x.com\",\"senha\":\"123456\"}";

    @Test
    void registerAdmin_atletaLogadaRecebe403() {
        client.mutateWith(mockUser("a@a.com").roles("ATLETA")).post().uri("/auth/register-admin")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(ADMIN_BODY)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void registerAdmin_anonimoRecebe401() {
        client.post().uri("/auth/register-admin")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(ADMIN_BODY)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void swagger_desligadoPorPadraoExigeLogin() {
        client.get().uri("/v3/api-docs").exchange().expectStatus().isUnauthorized();
        client.get().uri("/swagger-ui.html").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void modalidadesAdmin_anonimoRecebe401() {
        client.get().uri("/modalidades/admin").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void modalidadesAdmin_adminRecebe200() {
        when(modalidadeService.findAll()).thenReturn(Flux.empty());
        client.mutateWith(mockUser("adm@a.com").roles("ADMIN")).get().uri("/modalidades/admin")
                .exchange().expectStatus().isOk();
    }

    @Test
    void webhook_semTokenRecebe401() {
        client.post().uri("/webhooks/asaas").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"event\":\"PAYMENT_RECEIVED\"}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void webhook_tokenErradoRecebe401() {
        client.post().uri("/webhooks/asaas").header("asaas-access-token", "errado")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"event\":\"PAYMENT_RECEIVED\"}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void webhook_tokenCorretoRecebe200() {
        when(webhookService.processarEvento(any())).thenReturn(Mono.empty());
        client.post().uri("/webhooks/asaas").header("asaas-access-token", "segredo-de-teste")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"event\":\"PAYMENT_RECEIVED\"}")
                .exchange().expectStatus().isOk();
    }
}

@WebFluxTest(controllers = AsaasWebhookController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@TestPropertySource(properties = "app.swagger.enabled=true")
class SwaggerLigadoTest {

    @Autowired WebTestClient client;

    @MockBean UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean JwtService jwtService;
    @MockBean AsaasWebhookService webhookService;

    @Test
    void liberaSwaggerQuandoHabilitado() {
        // Sem springdoc no slice o recurso não existe (404), mas não é barrado pela segurança (401).
        client.get().uri("/v3/api-docs").exchange().expectStatus().isNotFound();
    }
}

@WebFluxTest(controllers = AsaasWebhookController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@TestPropertySource(properties = "asaas.webhook-token=")
class WebhookSemTokenConfiguradoTest {

    @Autowired WebTestClient client;

    @MockBean UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean JwtService jwtService;
    @MockBean AsaasWebhookService webhookService;

    @Test
    void falhaFechado() {
        client.post().uri("/webhooks/asaas").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"event\":\"PAYMENT_RECEIVED\"}")
                .exchange().expectStatus().isEqualTo(503);
    }
}
