package br.com.acervodaatletabrasileira.acervoapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * DTO para atualização das regras financeiras do acervo.
 * Permite a gestão dinâmica de taxas sem alteração de código.
 */
@Schema(description = "Dados para atualização das regras financeiras do acervo")
public record ConfiguracaoFiscalDTO(
        @Schema(example = "0.85", description = "Percentual que a atleta recebe (Ex: 0.85 para 85%)")
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal percentualRepasseAtleta,

        @Schema(example = "0.15", description = "Percentual que a plataforma retém (Ex: 0.15 para 15%)")
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal percentualComissaoPlataforma,

        /* =====================================================
           NOVOS CAMPOS DE GOVERNANÇA FINANCEIRA
           ===================================================== */

        @Schema(example = "0.05", description = "Taxa para o Fundo de Preservação (Itens Históricos/Domínio Público)")
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal taxaPreservacaoHistorica,

        @Schema(example = "1.50", description = "Custo fixo por transação para cobertura de Gas Fee da Blockchain")
        @DecimalMin("0.0") BigDecimal custoFixoBlockchain,

        @Schema(example = "Revisão anual de taxas conforme IPCA", description = "Justificativa ou nota legal")
        String observacaoLegal
) {
}