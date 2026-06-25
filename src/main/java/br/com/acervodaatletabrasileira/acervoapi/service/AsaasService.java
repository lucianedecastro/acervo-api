package br.com.acervodaatletabrasileira.acervoapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Serviço de integração com a API do ASAAS.
 *
 * RESPONSABILIDADE:
 * - Criar subconta (wallet) para atleta
 * - Criar cobrança com split automático
 * - Consultar status de cobrança
 *
 * IMPORTANTE:
 * - Não contém regra de negócio
 * - Não calcula valores
 * - Não valida jurídico
 * - Apenas integra com o gateway
 */
@Service
@Slf4j
public class AsaasService {

    private final WebClient webClient;

    public AsaasService(
            @Value("${asaas.base-url}") String baseUrl,
            @Value("${asaas.api-key}") String apiKey
    ) {

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("access_token", apiKey)
                .build();
    }

    /* =====================================================
       CRIAÇÃO DE SUBCONTA (WALLET DA ATLETA)
       ===================================================== */

    public Mono<Map> criarSubcontaAtleta(Map<String, Object> payload) {

        log.info("Criando subconta no ASAAS");

        return webClient.post()
                .uri("/accounts")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response ->
                        log.info("Subconta criada com sucesso: {}", response.get("id"))
                );
    }

    /* =====================================================
       CRIAÇÃO DE CLIENTE (COMPRADOR DA LICENÇA)
       ===================================================== */

    /**
     * Cria o "customer" do comprador no Asaas.
     *
     * Necessário porque toda cobrança no Asaas exige um customer
     * vinculado, e a plataforma não possui login para compradores
     * (cada licenciamento cria um customer avulso).
     */
    public Mono<Map> criarCliente(Map<String, Object> payload) {

        log.info("Criando cliente (comprador) no ASAAS");

        return webClient.post()
                .uri("/customers")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response ->
                        log.info("Cliente criado com sucesso: {}", response.get("id"))
                );
    }

    /* =====================================================
       CRIAÇÃO DE COBRANÇA COM SPLIT
       ===================================================== */

    public Mono<Map> criarCobrancaComSplit(Map<String, Object> payload) {

        log.info("Criando cobrança com split no ASAAS");

        return webClient.post()
                .uri("/payments")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response ->
                        log.info("Cobrança criada: {}", response.get("id"))
                );
    }

    /* =====================================================
       CONSULTA DE COBRANÇA
       ===================================================== */

    public Mono<Map> consultarCobranca(String paymentId) {

        return webClient.get()
                .uri("/payments/{id}", paymentId)
                .retrieve()
                .bodyToMono(Map.class);
    }

}