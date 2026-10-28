package br.com.acervodaatletabrasileira.acervoapi.security;

import br.com.acervodaatletabrasileira.acervoapi.config.GlobalExceptionHandler;
import br.com.acervodaatletabrasileira.acervoapi.config.JwtAuthFilter;
import br.com.acervodaatletabrasileira.acervoapi.config.SecurityConfig;
import br.com.acervodaatletabrasileira.acervoapi.controller.AtletaController;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.DocumentoLegalService;
import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import br.com.acervodaatletabrasileira.acervoapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = AtletaController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class AutocadastroTest {

    @Autowired WebTestClient client;

    @MockBean UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean JwtService jwtService;
    @MockBean AtletaService atletaService;
    @MockBean DocumentoLegalService documentoLegalService;

    private static Atleta atleta(String id, String nome, String statusAtleta) {
        Atleta a = new Atleta();
        a.setId(id);
        a.setNome(nome);
        a.setSlug(nome.toLowerCase());
        a.setStatusAtleta(statusAtleta);
        return a;
    }

    @Test
    void registro_anonimoComDadosValidosRecebe201() {
        Atleta salva = atleta("1", "Ana", AtletaService.STATUS_PENDENTE_APROVACAO);
        when(atletaService.registrar(any())).thenReturn(Mono.just(salva));

        client.post().uri("/atletas/registro").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"Ana\",\"email\":\"ana@x.com\",\"senha\":\"senhaforte1\",\"cpf\":\"123.456.789-01\",\"categoria\":\"ATIVA\"}")
                .exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isEqualTo("1");
    }

    @Test
    void registro_dadosInvalidosRecebe400() {
        client.post().uri("/atletas/registro").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nome\":\"\",\"email\":\"x\",\"senha\":\"123\"}")
                .exchange().expectStatus().isBadRequest()
                .expectBody().jsonPath("$.campos.email").exists().jsonPath("$.campos.senha").exists()
                .jsonPath("$.campos.cpf").exists();
        verifyNoInteractions(atletaService);
    }

    @Test
    void listaPublica_escondePerfilPendenteDeAprovacao() {
        when(atletaService.findAll()).thenReturn(Flux.just(
                atleta("1", "Aprovada", "ATIVO"),
                atleta("2", "Pendente", AtletaService.STATUS_PENDENTE_APROVACAO),
                atleta("3", "Antiga", null)));

        client.get().uri("/atletas").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[?(@.nome=='Pendente')]").isEmpty();
    }

    @Test
    void listaParaAdmin_incluiPerfilPendente() {
        when(atletaService.findAll()).thenReturn(Flux.just(
                atleta("1", "Aprovada", "ATIVO"),
                atleta("2", "Pendente", AtletaService.STATUS_PENDENTE_APROVACAO)));

        client.mutateWith(mockUser("adm@a.com").roles("ADMIN")).get().uri("/atletas")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void perfilPendente_publicoRecebe404() {
        when(atletaService.findById("2")).thenReturn(Mono.just(atleta("2", "Pendente", AtletaService.STATUS_PENDENTE_APROVACAO)));
        client.get().uri("/atletas/2").exchange().expectStatus().isNotFound();
    }
}
