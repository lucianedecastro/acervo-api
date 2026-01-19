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
    private String compradorId; // ID do usuário/empresa que licenciou

    private BigDecimal valorBrutoTotal;
    private BigDecimal percentualComissao;      // Ex: 20.00
    private BigDecimal valorComissaoPlataforma; // Valor que fica com o acervo
    private BigDecimal valorLiquidoRepasse;     // Valor que vai para a atleta/espólio

    private String tipoLicenca; // Ex: EDITORIAL, COMERCIAL, ACADEMICA
    private String moeda;       // Ex: BRL, USD
    private BigDecimal taxaCambio;

    private String statusFinanceiro; // PENDENTE, APROVADO, ESTORNADO, REPASSADO

    private String gatewayId;        // ID da transação no Stripe/Pagar.me
    private String comprovanteUrl;   // Link para o PDF da nota ou licença

    private Instant dataTransacao;
    private Instant atualizadoEm;
}