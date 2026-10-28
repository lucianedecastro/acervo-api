package br.com.acervodaatletabrasileira.acervoapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ResendEmailSender implements EmailSender {

    private final WebClient webClient;
    private final String apiKey;
    private final String remetente;

    public ResendEmailSender(
            WebClient.Builder builder,
            @Value("${resend.base-url:https://api.resend.com}") String baseUrl,
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:}") String remetente
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.remetente = remetente;
    }

    @Override
    public Mono<Void> enviar(String para, String assunto, String html, String texto) {
        if (apiKey == null || apiKey.isBlank() || remetente == null || remetente.isBlank()) {
            log.warn("E-mail NÃO enviado: 'resend.api-key' e/ou 'resend.from' não configurados");
            return Mono.empty();
        }

        return webClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "from", remetente,
                        "to", List.of(para),
                        "subject", assunto,
                        "html", html,
                        "text", texto
                ))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(15))
                .doOnError(e -> log.error("Falha ao enviar e-mail pelo Resend: {}", e.getMessage()))
                .then();
    }
}
