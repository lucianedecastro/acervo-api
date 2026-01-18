package br.com.acervodaatletabrasileira.acervoapi.dto;

public record AtletaDashboardStatsDTO(
        long totalMeusItens,
        long itensPublicados,
        long itensEmRascunho,
        long totalVisualizacoesPerfil // Futuro campo de métricas
) {}
