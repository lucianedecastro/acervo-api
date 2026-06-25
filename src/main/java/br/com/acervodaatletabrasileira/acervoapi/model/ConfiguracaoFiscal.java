package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modelo de Configuração Fiscal e Operacional.
 * Define as regras de divisão de valores e custos do ecossistema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "configuracoes_fiscais")
public class ConfiguracaoFiscal {

    @Id
    private String id; // Padrão: "GLOBAL_SETTINGS"

    /* =====================================================
       PERCENTUAIS DE REPASSE (ALINHADO AO SERVICE)
       ===================================================== */

    private BigDecimal percentualRepasseAtleta;

    private BigDecimal percentualComissaoPlataforma;

    private BigDecimal taxaPreservacaoHistorica;

    /* =====================================================
       CUSTOS OPERACIONAIS
       ===================================================== */

    private BigDecimal custoFixoBlockchain;

    /* =====================================================
       AUDITORIA E CONTROLE
       ===================================================== */

    private String observacaoLegal;

    private Instant atualizadoEm;

    private String atualizadoPor;
}