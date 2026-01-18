package br.com.acervodaatletabrasileira.acervoapi.model;

/**
 * Controla o ciclo de vida do item na plataforma,
 * garantindo a segurança jurídica antes do licenciamento.
 * Agora com suporte à diferenciação entre memorial histórico e licenciamento ativo.
 */
public enum StatusItemAcervo {
    RASCUNHO,                 // Em edição pelo curador
    EM_ANALISE_JURIDICA,      // Aguardando validação de direitos autorais ou imagem
    PUBLICADO,                // Visível para consulta pública (frente ativa)
    DISPONIVEL_LICENCIAMENTO, // Aprovado para circulação remunerada (venda)
    MEMORIAL,                 // Item exclusivo para pesquisa histórica (sem licenciamento comercial)
    ARQUIVADO                 // Item removido da visualização, mas mantido para histórico
}