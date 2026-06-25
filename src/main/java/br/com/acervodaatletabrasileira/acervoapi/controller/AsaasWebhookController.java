package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.service.AsaasWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Recebe os callbacks de pagamento enviados pelo Asaas.
 *
 * SEGURANÇA:
 * - Rota pública no SecurityConfig (o Asaas não envia JWT).
 * - Validação feita aqui via header "asaas-access-token",
 *   que deve corresponder ao token configurado no painel do Asaas
 *   (propriedade "asaas.webhook-token").
 *
 * Oculto do Swagger público por não ser uma rota de uso humano.
 */
@Hidden
@RestController
@RequestMapping("/webhooks/asaas")
@Slf4j
public class AsaasWebhookController {

    private final AsaasWebhookService webhookService;

    @Value("${asaas.webhook-token:}")
    private String webhookTokenEsperado;

    public AsaasWebhookController(AsaasWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> receberEvento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody Map<String, Object> payload
    ) {
        if (webhookTokenEsperado != null && !webhookTokenEsperado.isBlank()
                && !webhookTokenEsperado.equals(tokenRecebido)) {
            log.warn("Webhook Asaas recebido com token inválido");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return webhookService.processarEvento(payload)
                .thenReturn(ResponseEntity.ok().<Void>build())
                .onErrorResume(erro -> {
                    // Responde 200 mesmo em erro interno para evitar reentrega
                    // agressiva do Asaas; o erro fica registrado para investigação.
                    log.error("Erro ao processar webhook Asaas", erro);
                    return Mono.just(ResponseEntity.ok().build());
                });
    }
}
