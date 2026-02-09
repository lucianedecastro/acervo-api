package br.com.acervodaatletabrasileira.acervoapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representa os metadados de uma foto do acervo.
 *
 * - publicId: identificador único no storage (Cloudinary)
 * - version: versão do asset no Cloudinary (obrigatória para delivery)
 * - url: URL original pública (fallback de segurança)
 *
 * Campos adicionais são opcionais e não obrigam uso no frontend.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FotoDTO(

        // Identificador interno opcional (não utilizado atualmente)
        String id,

        // ID único no Cloudinary
        String publicId,

        /**
         * Versão do asset no Cloudinary
         * Ex: 1770640528 → frontend monta "v1770640528"
         */
        Long version,

        // Legenda editorial
        String legenda,

        // Indica se é a imagem de destaque do item
        Boolean ehDestaque,

        /**
         * URL original persistida (sem marca d’água)
         * Usada apenas como fallback de segurança
         */
        String url,

        // Nome do arquivo original
        String filename,

        /* =====================================================
           CAMPOS OPCIONAIS (EXTENSÃO FUTURA)
           ===================================================== */

        // Nome público do autor (fotógrafa, agência, arquivo histórico)
        String autorNomePublico,

        // Indica se a imagem pode ser licenciada individualmente
        Boolean licenciamentoPermitido
) {

    /**
     * Criação de DTO após upload simples (Cloudinary)
     */
    public static FotoDTO fromUpload(
            String url,
            String publicId,
            Long version,
            String legenda,
            Boolean ehDestaque
    ) {
        return new FotoDTO(
                null,
                publicId,
                version,
                legenda,
                ehDestaque,
                url,
                null,
                null,
                null
        );
    }
}
