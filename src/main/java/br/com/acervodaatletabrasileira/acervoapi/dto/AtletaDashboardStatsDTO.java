package br.com.acervodaatletabrasileira.acervoapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo estatístico individual da atleta para o Dashboard")
public record AtletaDashboardStatsDTO(
        @Schema(description = "Total de itens vinculados à atleta no acervo")
        long totalMeusItens,

        @Schema(description = "Quantidade de itens já aprovados e publicados")
        long itensPublicados,

        @Schema(description = "Quantidade de itens que ainda estão em edição (rascunho)")
        long itensEmRascunho,

        @Schema(description = "Quantidade de itens históricos ou em memorial")
        long itensNoMemorial,

        @Schema(description = "Total de visualizações do perfil público (métrica futura)")
        long totalVisualizacoesPerfil
) {
    // Construtor auxiliar para o Service que usamos agora (sem as visualizações ainda)
    public AtletaDashboardStatsDTO(long totalMeusItens, long itensPublicados, long itensEmRascunho, long itensNoMemorial) {
        this(totalMeusItens, itensPublicados, itensEmRascunho, itensNoMemorial, 0L);
    }
}