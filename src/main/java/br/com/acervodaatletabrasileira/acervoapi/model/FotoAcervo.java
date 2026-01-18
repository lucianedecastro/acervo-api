package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa os arquivos digitais associados a um Item do Acervo.
 * Adaptado para suportar o fluxo de licenciamento (visualização vs. entrega).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoAcervo {

    /**
     * Identificador único no Cloudinary (publicId)
     */
    private String publicId;

    /**
     * URL da imagem com marca d'água ou baixa resolução
     * (Usada na galeria pública do site)
     */
    private String urlVisualizacao;

    /**
     * URL da imagem original em alta resolução
     * (Acesso restrito, liberado apenas após o licenciamento)
     */
    private String urlAltaResolucao;

    /**
     * Legenda específica para esta imagem/documento
     */
    private String legenda;

    /**
     * Nome do arquivo original (ex: foto_olimpiada_2004.jpg)
     */
    private String nomeArquivo;

    /**
     * Indica se este arquivo é o principal/capa do item
     */
    private boolean destaque;
}