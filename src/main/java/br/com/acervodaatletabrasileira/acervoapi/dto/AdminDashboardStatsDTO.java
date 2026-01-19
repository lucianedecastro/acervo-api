package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminDashboardStatsDTO(
        long totalAtletas,
        long totalItensAcervo,
        long totalModalidades,
        long itensAguardandoPublicacao,
        Map<String, Long> itensPorTipo,

        // Novos campos financeiros para a gestão
        BigDecimal faturamentoTotalBruto,
        BigDecimal totalComissoesPlataforma
) {}