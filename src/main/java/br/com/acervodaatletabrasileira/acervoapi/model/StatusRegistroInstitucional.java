package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Status do registro institucional imutável.
 *
 * IMPORTANTE:
 * Não representa status financeiro.
 * Controla apenas o ciclo de prova institucional em Blockchain.
 */
public enum StatusRegistroInstitucional {

    NAO_REGISTRADO,
    PENDENTE_REGISTRO,
    REGISTRADO_COM_SUCESSO,
    FALHA_NO_REGISTRO
}

