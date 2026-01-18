package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Define a natureza física ou digital do item preservado pela atleta.
 * Baseado na constatação de que o acervo pessoal é multifacetado.
 */
public enum TipoItemAcervo {
    FOTO,
    VIDEO,
    AUDIO,
    DOCUMENTO_OFICIAL, // Ex: Passaportes, Convocações
    CREDENCIAL,        // Ex: Credenciais de Jogos Olímpicos ou Pan-Americanos
    RECORTE_IMPRENSA,  // Ex: Páginas de jornais e revistas da época
    OBJETO_FISICO      // Ex: Medalhas, Troféus ou Uniformes (para catalogação)
}