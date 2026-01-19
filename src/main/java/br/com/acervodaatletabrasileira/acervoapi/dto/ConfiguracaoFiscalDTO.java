package br.com.acervodaatletabrasileira.acervoapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Dados para atualização das regras financeiras do acervo")
public record ConfiguracaoFiscalDTO(
        @Schema(example = "0.85", description = "Percentual que a atleta recebe (Ex: 0.85 para 85%)")
        BigDecimal percentualRepasseAtleta,

        @Schema(example = "0.15", description = "Percentual que a plataforma retém (Ex: 0.15 para 15%)")
        BigDecimal percentualComissaoPlataforma,

        @Schema(example = "Revisão anual de taxas conforme IPCA", description = "Justificativa ou nota legal")
        String observacaoLegal
) {
}
