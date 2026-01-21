package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Ciclo de vida de um licenciamento.
 */
public enum StatusLicenciamento {

    SOLICITADO,          // Proposta criada
    EM_ANALISE_JURIDICA, // Jurídico avaliando
    APROVADO,            // Autorizado para uso
    NEGADO,              // Bloqueado juridicamente
    CANCELADO,           // Cancelado administrativamente
    FINALIZADO           // Licenciamento expirado
}

