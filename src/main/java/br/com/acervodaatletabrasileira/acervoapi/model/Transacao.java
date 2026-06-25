package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representa o registro financeiro de um licenciamento.
 *
 * IMPORTANTE:
 * - Este model NÃO executa pagamento.
 * - Não representa intermediação bancária.
 * - Apenas registra o evento financeiro ocorrido via Gateway.
 *
 * A Blockchain é utilizada exclusivamente como
 * camada institucional de prova imutável.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transacoes")
public class Transacao {

    @Id
    private String id;

    /* =====================================================
       IDENTIFICAÇÃO DO EVENTO
       ===================================================== */

    private String itemId;
    private String atletaId;
    private String compradorId; // ID do comprador (quando houver login futuro)

    /* =====================================================
       VALORES FINANCEIROS
       ===================================================== */

    private BigDecimal valorBrutoTotal;

    /**
     * Percentual de comissão da plataforma.
     * Ex: 0.15 (15%)
     */
    private BigDecimal percentualComissao;

    /**
     * Valor final retido pela plataforma.
     */
    private BigDecimal valorComissaoPlataforma;

    /**
     * Valor líquido destinado à atleta/espólio.
     * Este valor é transferido diretamente pelo Gateway (Split).
     */
    private BigDecimal valorLiquidoRepasse;

    /* =====================================================
       INFORMAÇÕES DA COBRANÇA
       ===================================================== */

    private String tipoLicenca; // EDITORIAL, COMERCIAL, etc
    private String moeda;       // BRL, USD...
    private BigDecimal taxaCambio;

    /**
     * Status retornado pelo Gateway.
     * Ex: PENDENTE, CONFIRMADA, CANCELADA
     */
    private String statusFinanceiro;

    /**
     * Identificador da transação no Gateway (Asaas).
     */
    private String gatewayId;

    /**
     * Identificador do cliente (comprador) no Asaas.
     * Não há módulo de login para compradores, então cada
     * licenciamento cria/reaproveita um customer no gateway.
     */
    private String gatewayCustomerId;

    /**
     * Link da fatura/cobrança no Asaas (checkout hospedado).
     * Entregue ao comprador para concluir o pagamento.
     * Continua sendo o mesmo link após a confirmação,
     * servindo também como comprovante.
     */
    private String linkPagamento;

    /**
     * URL de comprovante ou nota fiscal (quando aplicável).
     */
    private String comprovanteUrl;

    /* =====================================================
       PROVA INSTITUCIONAL (BLOCKCHAIN)
       ===================================================== */

    /**
     * Hash da transação na rede Blockchain.
     * Representa prova pública e imutável
     * da execução financeira.
     */
    private String blockchainTxId;

    /**
     * Status do registro institucional.
     * Controla apenas o ciclo da prova imutável.
     */
    private StatusRegistroInstitucional statusRegistroInstitucional;

    /**
     * Data de confirmação do registro institucional.
     */
    private Instant dataRegistroBlockchain;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant dataTransacao;
    private Instant atualizadoEm;
}