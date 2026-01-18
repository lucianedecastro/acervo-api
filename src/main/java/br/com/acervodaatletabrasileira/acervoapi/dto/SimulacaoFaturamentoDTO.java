package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.math.BigDecimal;

public record SimulacaoFaturamentoDTO(
        String itemTitulo,
        BigDecimal valorTotal,
        BigDecimal repasseAtleta, // Valor que a Marta recebe
        BigDecimal comissaoPlataforma, // Valor que fica para o Acervo
        String chavePixAtleta // Para onde o dinheiro vai
) {}