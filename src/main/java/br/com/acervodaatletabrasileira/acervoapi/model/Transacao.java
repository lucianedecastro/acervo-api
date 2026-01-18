package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transacoes")
public class Transacao {

    @Id
    private String id;

    private String itemId;
    private String atletaId;
    private String compradorId;

    private BigDecimal valorBrutoTotal;
    private BigDecimal percentualComissao;
    private BigDecimal valorComissaoPlataforma;
    private BigDecimal valorLiquidoRepasse;

    private String tipoLicenca;
    private String moeda;
    private BigDecimal taxaCambio;

    private String statusFinanceiro;
    private Instant dataTransacao;
}