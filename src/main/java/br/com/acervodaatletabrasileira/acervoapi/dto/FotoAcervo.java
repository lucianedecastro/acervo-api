package br.com.acervodaatletabrasileira.acervoapi.dto;

/**
 * DTO para representar uma foto do acervo, contendo a URL no GCS e sua legenda.
 */
public record FotoAcervo(
        String url,
        String legenda
) {}
