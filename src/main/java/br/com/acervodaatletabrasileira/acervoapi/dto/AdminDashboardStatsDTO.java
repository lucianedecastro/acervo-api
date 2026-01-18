package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.util.Map;

public record AdminDashboardStatsDTO(
        long totalAtletas,
        long totalItensAcervo,
        long totalModalidades,
        long itensAguardandoPublicacao,
        Map<String, Long> itensPorTipo
) {}