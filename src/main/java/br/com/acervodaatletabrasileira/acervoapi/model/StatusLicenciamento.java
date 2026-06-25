package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Ciclo de vida jurídico de um licenciamento.
 *
 * IMPORTANTE:
 * Este enum controla apenas o estado jurídico do licenciamento.
 * O registro em Blockchain é tratado separadamente
 * via campos específicos (blockchainTxId e dataRegistroBlockchain).
 */
public enum StatusLicenciamento {

    SOLICITADO,          // Proposta criada
    EM_ANALISE_JURIDICA, // Jurídico avaliando
    APROVADO,            // Autorizado para uso
    NEGADO,              // Bloqueado juridicamente
    CANCELADO,           // Cancelado administrativamente
    FINALIZADO           // Licenciamento expirado
}
