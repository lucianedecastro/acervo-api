package br.com.acervodaatletabrasileira.acervoapi.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResendEmailSenderTest {

    @Test
    void semChaveConfiguradaNaoChamaOResendENaoFalha() {
        List<ClientRequest> chamadas = new ArrayList<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(req -> {
            chamadas.add(req);
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        });

        new ResendEmailSender(builder, "https://api.resend.com", "", "")
                .enviar("a@a.com", "assunto", "<p>oi</p>", "oi").block();

        assertThat(chamadas).isEmpty();
    }

    @Test
    void comChaveEnviaPostAutenticadoParaOEndpointDeEmails() {
        List<ClientRequest> chamadas = new ArrayList<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(req -> {
            chamadas.add(req);
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
        });

        new ResendEmailSender(builder, "https://api.resend.com", "re_chave", "Acervo <nao-responda@exemplo.com>")
                .enviar("a@a.com", "assunto", "<p>oi</p>", "oi").block();

        assertThat(chamadas).hasSize(1);
        ClientRequest req = chamadas.get(0);
        assertThat(req.method().name()).isEqualTo("POST");
        assertThat(req.url().toString()).isEqualTo("https://api.resend.com/emails");
        assertThat(req.headers().getFirst("Authorization")).isEqualTo("Bearer re_chave");
    }
}
