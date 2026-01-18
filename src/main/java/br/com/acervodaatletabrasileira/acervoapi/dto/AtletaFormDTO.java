package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.math.BigDecimal;
import java.util.List;

public record AtletaFormDTO(
        String nome,
        String nomeSocial,
        List<String> modalidades,
        String biografia,
        Boolean contratoAssinado,
        String linkContratoDigital,
        String dadosContato,
        String informacoesParaRepasse,
        BigDecimal comissaoPlataformaDiferenciada, // Mude de Double para BigDecimal aqui
        String fotoDestaqueId,
        String statusAtleta
) {}