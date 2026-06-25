package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Transacao;
import br.com.acervodaatletabrasileira.acervoapi.repository.TransacaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Processa os eventos recebidos via webhook do Asaas.
 *
 * RESPONSABILIDADE:
 * - Localizar a Transacao correspondente (via externalReference)
 * - Atualizar o status financeiro local
 *
 * IMPORTANTE:
 * - NÃO toma decisão jurídica.
 * - NÃO registra na Blockchain (isso é um ato institucional separado,
 *   acionado manualmente via Governança/Admin).
 * - É idempotente: receber o mesmo evento duas vezes não duplica efeito,
 *   só reescreve o mesmo status.
 */
@Service
@Slf4j
public class AsaasWebhookService {

    private final TransacaoRepository transacaoRepository;

    private static final Set<String> EVENTOS_CONFIRMACAO = Set.of(
            "PAYMENT_CONFIRMED",
            "PAYMENT_RECEIVED"
    );

    private static final Set<String> EVENTOS_CANCELAMENTO = Set.of(
            "PAYMENT_DELETED",
            "PAYMENT_REFUNDED",
            "PAYMENT_OVERDUE"
    );

    public AsaasWebhookService(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    @SuppressWarnings("unchecked")
    public Mono<Void> processarEvento(Map<String, Object> payload) {

        String evento = (String) payload.get("event");
        Object paymentObj = payload.get("payment");

        if (evento == null || !(paymentObj instanceof Map)) {
            log.warn("Webhook Asaas recebido em formato inesperado: {}", payload);
            return Mono.empty();
        }

        Map<String, Object> payment = (Map<String, Object>) paymentObj;
        Object externalReference = payment.get("externalReference");

        if (externalReference == null || externalReference.toString().isBlank()) {
            log.warn("Webhook Asaas sem externalReference, evento: {}", evento);
            return Mono.empty();
        }

        String transacaoId = externalReference.toString();

        if (EVENTOS_CONFIRMACAO.contains(evento)) {
            return atualizarStatus(transacaoId, "LIQUIDADA", payment);
        }

        if (EVENTOS_CANCELAMENTO.contains(evento)) {
            return atualizarStatus(transacaoId, "CANCELADA_GATEWAY", payment);
        }

        log.info("Evento Asaas '{}' recebido e ignorado (sem ação mapeada) para transação {}", evento, transacaoId);
        return Mono.empty();
    }

    private Mono<Void> atualizarStatus(String transacaoId, String novoStatus, Map<String, Object> payment) {
        return transacaoRepository.findById(transacaoId)
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Webhook Asaas: transação {} não encontrada localmente", transacaoId)
                ))
                .flatMap(transacao -> {
                    transacao.setStatusFinanceiro(novoStatus);

                    Object invoiceUrl = payment.get("invoiceUrl");
                    if (invoiceUrl != null) {
                        transacao.setLinkPagamento(invoiceUrl.toString());
                    }

                    transacao.setAtualizadoEm(Instant.now());
                    return transacaoRepository.save(transacao);
                })
                .doOnNext(t -> log.info("Transação {} atualizada para status '{}'", t.getId(), novoStatus))
                .then();
    }
}
