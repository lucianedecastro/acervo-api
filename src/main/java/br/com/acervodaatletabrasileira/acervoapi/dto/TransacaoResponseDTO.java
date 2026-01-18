package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransacaoResponseDTO(
        String id,
        String itemAcervoId,
        String atletaId,
        BigDecimal valorTotal,
        BigDecimal valorRepasseAtleta,
        Instant dataTransacao,
        String status,
        String tipoLicenca
) {}
