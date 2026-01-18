package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExtratoAtletaDTO(
        String nomeAtleta,
        BigDecimal saldoTotal,
        List<TransacaoResponseDTO> transacoes
) {}
