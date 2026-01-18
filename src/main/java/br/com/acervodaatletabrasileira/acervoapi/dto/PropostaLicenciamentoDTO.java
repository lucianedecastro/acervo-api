package br.com.acervodaatletabrasileira.acervoapi.dto;

public record PropostaLicenciamentoDTO(
        String itemAcervoId,
        String atletaId,
        String tipoUso, // Ex: COMERCIAL, EDITORIAL
        Integer prazoMeses
) {}
