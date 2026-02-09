package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um arquivo digital associado a um Item do Acervo.
 * Não depende de fotógrafa, atleta ou licenciamento específico.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoAcervo {

    /**
     * Identificador único no storage (ex: Cloudinary publicId)
     */
    private String publicId;

    /**
     * Versão do arquivo no storage (Cloudinary version).
     * Usada para garantir URLs imutáveis e evitar cache incorreto.
     */
    private Long version;

    /**
     * URL pública (marca d'água, preview ou baixa resolução)
     */
    private String urlVisualizacao;

    /**
     * URL restrita (alta resolução, entrega pós-licenciamento)
     */
    private String urlAltaResolucao;

    /**
     * Nome original do arquivo
     */
    private String nomeArquivo;

    /**
     * Legenda editorial
     */
    private String legenda;

    /**
     * Indica se esta imagem é a capa do item
     */
    private boolean destaque;

    /* =====================================================
       METADADOS OPCIONAIS (NÃO OBRIGATÓRIOS)
       ===================================================== */

    /**
     * Indica se a imagem possui marca d’água aplicada
     */
    private boolean possuiMarcaDagua;

    /**
     * Identificador do autor da imagem (fotógrafa, arquivo, agência etc)
     * Livre, não amarrado a entidade específica
     */
    private String autorId;

    /**
     * Nome público do autor (exibível mesmo sem entidade interna)
     */
    private String autorNomePublico;

    /**
     * Indica se esta imagem pode ser licenciada
     * (controle fino por imagem)
     */
    private boolean licenciamentoPermitido;
}
