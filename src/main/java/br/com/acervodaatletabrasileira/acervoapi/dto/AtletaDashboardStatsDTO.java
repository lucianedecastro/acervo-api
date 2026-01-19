package br.com.acervodaatletabrasileira.acervoapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Resumo estatístico e financeiro individual da atleta para o Dashboard")
public record AtletaDashboardStatsDTO(
        @Schema(description = "Total de itens vinculados à atleta no acervo")
        long totalMeusItens,

        @Schema(description = "Quantidade de itens já aprovados e publicados")
        long itensPublicados,

        @Schema(description = "Quantidade de itens que ainda estão em edição (rascunho)")
        long itensEmRascunho,

        @Schema(description = "Quantidade de itens históricos ou em memorial")
        long itensNoMemorial,

        @Schema(description = "Total de licenças/vendas realizadas")
        long totalLicenciamentosVendidos,

        @Schema(description = "Saldo total líquido disponível para repasse")
        BigDecimal saldoTotalAtleta
) {
}