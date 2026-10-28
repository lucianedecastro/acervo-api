package br.com.acervodaatletabrasileira.acervoapi.security;

import br.com.acervodaatletabrasileira.acervoapi.config.GlobalExceptionHandler;
import br.com.acervodaatletabrasileira.acervoapi.config.JwtAuthFilter;
import br.com.acervodaatletabrasileira.acervoapi.config.SecurityConfig;
import br.com.acervodaatletabrasileira.acervoapi.controller.AuthController;
import br.com.acervodaatletabrasileira.acervoapi.controller.ConfiguracaoFiscalController;
import br.com.acervodaatletabrasileira.acervoapi.controller.FotografaController;
import br.com.acervodaatletabrasileira.acervoapi.controller.LicenciamentoController;
import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConfiguracaoFiscalRepository;
import br.com.acervodaatletabrasileira.acervoapi.service.AuthService;
import br.com.acervodaatletabrasileira.acervoapi.service.FotografaService;
import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import br.com.acervodaatletabrasileira.acervoapi.service.LicenciamentoService;
import br.com.acervodaatletabrasileira.acervoapi.service.UserDetailsServiceImpl;
import br.com.acervodaatletabrasileira.acervoapi.service.UsuarioAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = {FotografaController.class, LicenciamentoController.class,
        ConfiguracaoFiscalController.class, AuthController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class ValidacaoEntradaTest {

    @Autowired WebTestClient client;

    @MockBean UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean JwtService jwtService;
    @MockBean AuthService authService;
    @MockBean UsuarioAdminService usuarioAdminService;
    @MockBean FotografaService fotografaService;
    @MockBean LicenciamentoService licenciamentoService;
    @MockBean ConfiguracaoFiscalRepository configuracaoFiscalRepository;

    @Test
    void cadastroFotografa_semEmailSenhaCurtaRecebe400ComCampos() {
        client.post().uri("/fotografas/cadastro").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"Ana\",\"email\":\"nao-e-email\",\"senha\":\"123\"}")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.campos.email").exists().jsonPath("$.campos.senha").exists();
        verifyNoInteractions(fotografaService);
    }

    @Test
    void cadastroFotografa_ignoraCamposPrivilegiadosEnaoDevolveSenha() {
        when(fotografaService.createCadastro(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        client.post().uri("/fotografas/cadastro").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"nome":"Ana","email":"ANA@Teste.com","senha":"senhaforte1",
                         "statusVerificacao":"APROVADO","statusFotografa":"ATIVA",
                         "percentualRepasse":1.0,"gatewayAccountId":"conta-alheia","contratoAssinado":true}
                        """)
                .exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.senha").doesNotExist();

        ArgumentCaptor<Fotografa> captor = ArgumentCaptor.forClass(Fotografa.class);
        verify(fotografaService).createCadastro(captor.capture());
        Fotografa f = captor.getValue();
        assertThat(f.getEmail()).isEqualTo("ana@teste.com");
        assertThat(f.getPercentualRepasse()).isNull();
        assertThat(f.getGatewayAccountId()).isNull();
        assertThat(f.getStatusVerificacao()).isEqualTo(Fotografa.StatusVerificacao.PENDENTE);
        assertThat(f.getContratoAssinado()).isFalse();
    }

    @Test
    void efetivar_semItemOuAtletaRecebe400() {
        client.mutateWith(mockUser("a@a.com").roles("ATLETA")).post().uri("/licenciamento/efetivar")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"prazoMeses\":-1}")
                .exchange().expectStatus().isBadRequest();
        verifyNoInteractions(licenciamentoService);
    }

    @Test
    void configuracaoFiscal_percentualAcimaDe100PorCentoRecebe400() {
        client.mutateWith(mockUser("adm@a.com").roles("ADMIN")).put().uri("/configuracoes/fiscal")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"percentualRepasseAtleta\":85}")
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void registerAdmin_senhaCurtaRecebe400_mesmoParaAdmin() {
        client.mutateWith(mockUser("adm@a.com").roles("ADMIN")).post().uri("/auth/register-admin")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"email\":\"x@x.com\",\"senha\":\"123\"}")
                .exchange().expectStatus().isBadRequest();
        verifyNoInteractions(usuarioAdminService);
    }
}
