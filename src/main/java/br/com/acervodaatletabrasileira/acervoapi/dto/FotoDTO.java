package br.com.acervodaatletabrasileira.acervoapi.dto;

/**
 * Representa os metadados de uma foto do acervo.
 *
 * - id: identificador da foto (opcional, só existe após persistência)
 * - legenda: texto descritivo da imagem
 * - ehDestaque: indica se a foto é principal
 * - url: URL pública da imagem (Cloudinary, S3, etc.)
 * - filename: nome do arquivo enviado no upload (multipart)
 */
public record FotoDTO(
        String id,          // pode ser null
        String legenda,
        Boolean ehDestaque, // Boolean (não boolean)
        String url,
        String filename
) {
}
