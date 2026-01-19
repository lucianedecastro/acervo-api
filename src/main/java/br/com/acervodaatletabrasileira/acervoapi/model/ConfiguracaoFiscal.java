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
@Document(collection = "configuracoes_fiscais")
public class ConfiguracaoFiscal {

    @Id
    private String id; // Usaremos um ID fixo como "GLOBAL_SETTINGS"

    // Percentuais de Divisão
    private BigDecimal percentualRepasseAtleta;    // Ex: 0.85
    private BigDecimal percentualComissaoPlataforma; // Ex: 0.15

    // Campos para futura expansão fiscal (ISS, nota técnica, etc)
    private String observacaoLegal;

    private Instant atualizadoEm;
    private String atualizadoPor; // E-mail do Admin que alterou
}