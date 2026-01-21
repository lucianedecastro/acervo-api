package br.com.acervodaatletabrasileira.acervoapi.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Controla o ciclo de vida jurídico e editorial do item do acervo.
 */
public enum StatusItemAcervo {

    RASCUNHO,                 // Em edição
    EM_ANALISE_JURIDICA,      // Direitos autorais / imagem
    PUBLICADO,                // Visível (consulta)
    DISPONIVEL_LICENCIAMENTO, // Venda permitida
    MEMORIAL,                 // Histórico, sem exploração comercial
    ARQUIVADO;                // Retido por auditoria ou histórico

    /**
     * Status visíveis publicamente
     */
    private static final Set<StatusItemAcervo> STATUS_PUBLICOS =
            EnumSet.of(PUBLICADO, DISPONIVEL_LICENCIAMENTO, MEMORIAL);

    /**
     * Indica se o item pode gerar receita
     */
    public boolean podeGerarReceita() {
        return this == DISPONIVEL_LICENCIAMENTO;
    }

    /**
     * Indica se o item é visível publicamente
     */
    public boolean visivelPublicamente() {
        return STATUS_PUBLICOS.contains(this);
    }

    /**
     * Lista padrão de status públicos
     * (uso direto em services e repositories)
     */
    public static Set<StatusItemAcervo> statusPublicos() {
        return STATUS_PUBLICOS;
    }
}
