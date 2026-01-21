package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Define a natureza do item preservado no acervo.
 * Mantido genérico para suportar atletas, fotógrafas e memória institucional.
 */
public enum TipoItemAcervo {

    FOTO,
    VIDEO,
    AUDIO,

    DOCUMENTO_OFICIAL,
    CREDENCIAL,
    RECORTE_IMPRENSA,
    OBJETO_FISICO;

    /**
     * Indica se o tipo é naturalmente licenciável
     */
    public boolean podeSerLicenciado() {
        return this == FOTO || this == VIDEO || this == AUDIO;
    }
}
