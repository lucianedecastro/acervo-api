package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Controla o estado técnico do registro institucional
 * de integridade na Blockchain.
 *
 * IMPORTANTE:
 * Este enum NÃO representa estado jurídico nem editorial.
 * Ele controla exclusivamente o ciclo técnico de registro
 * da prova de imutabilidade.
 *
 * Fluxo típico:
 *
 * NAO_REGISTRADO  → hash ainda não enviado
 * PENDENTE        → transação enviada, aguardando confirmação
 * REGISTRADO      → confirmado na rede
 * FALHA_REGISTRO  → erro técnico (reprocessamento necessário)
 */
public enum StatusBlockchain {

    NAO_REGISTRADO,
    PENDENTE,
    REGISTRADO,
    FALHA_REGISTRO
}

