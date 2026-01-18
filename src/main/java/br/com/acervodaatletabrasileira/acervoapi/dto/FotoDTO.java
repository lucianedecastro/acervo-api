package br.com.acervodaatletabrasileira.acervoapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Representa os metadados de uma foto do acervo.
 * * - publicId: ID único no Cloudinary (essencial para gestão da mídia)
 * - url: Endereço público da imagem
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FotoDTO(
        String id,
        String publicId,
        String legenda,
        Boolean ehDestaque,
        String url,
        String filename
) {
    // Método auxiliar para criar um DTO de resposta após o upload do Cloudinary
    public static FotoDTO fromUpload(String url, String publicId, String legenda, Boolean ehDestaque) {
        return new FotoDTO(null, publicId, legenda, ehDestaque, url, null);
    }
}