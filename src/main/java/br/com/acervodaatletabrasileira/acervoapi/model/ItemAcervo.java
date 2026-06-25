package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Representa um ativo do Acervo da Atleta Brasileira.
 * Suporta preservação histórica, curadoria editorial e licenciamento comercial.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "itens_acervo")
public class ItemAcervo {

    @Id
    private String id;

    /* =====================================================
       IDENTIDADE EDITORIAL / HISTÓRICA
       ===================================================== */

    private String titulo;
    private String descricao;
    private String local;

    /**
     * Data histórica livre (ex: "Junho de 2004", "Década de 1920")
     */
    private String dataOriginal;

    /* =====================================================
       PROCEDÊNCIA E CRÉDITOS
       ===================================================== */

    /**
     * Origem do material (ex: "Acervo pessoal", "Domínio Público", "Arquivo Institucional")
     */
    private String procedencia;

    /**
     * Crédito autoral exibível publicamente
     */
    private String creditoAutoral;

    /**
     * Identificador interno opcional do autor
     */
    private String autorId;

    /* =====================================================
       PESQUISA E MEMÓRIA
       ===================================================== */

    private String fontePesquisa;
    private String linkFontePesquisa;

    /**
     * Indica se a obra já caiu em domínio público
     */
    private Boolean dominioPublico;

    /* =====================================================
       TIPOLOGIA E STATUS
       ===================================================== */

    private TipoItemAcervo tipo;
    private StatusItemAcervo status;

    /* =====================================================
       RELACIONAMENTOS
       ===================================================== */

    private String modalidadeId;
    private List<String> atletasIds;

    /* =====================================================
       CONTROLE DE LICENCIAMENTO
       ===================================================== */

    private Boolean disponivelParaLicenciamento;
    private BigDecimal precoBaseLicenciamento;
    private Boolean itemHistorico;
    private String restricoesUso;

    /* =====================================================
       DIREITOS AUTORAIS / IMAGEM
       ===================================================== */

    private Boolean possuiDocumentacaoDireitos;
    private String documentoDireitosId;

    /* =====================================================
       ARQUIVOS DIGITAIS
       ===================================================== */

    private List<FotoAcervo> fotos;

    /* =====================================================
       RASTREABILIDADE INSTITUCIONAL (BLOCKCHAIN)
       ===================================================== */

    /**
     * Hash SHA-256 do arquivo principal.
     * Garantia técnica de integridade.
     */
    private String blockchainContentHash;

    /**
     * ID da Transação (TxId) na rede Blockchain.
     * Prova pública de existência.
     */
    private String blockchainTxId;

    /**
     * Data em que o registro foi confirmado na rede.
     */
    private Instant dataRegistroBlockchain;

    /**
     * Status técnico do registro institucional.
     */
    private StatusBlockchain statusBlockchain;

    /* =====================================================
       INTELIGÊNCIA FINANCEIRA / AUDITORIA
       ===================================================== */

    private BigDecimal percentualRepasseNoMomento;
    private String loteFinanceiroId;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;
    private Instant atualizadoEm;
    private String curadorResponsavel;

    /* =====================================================
       REGRAS DE NEGÓCIO
       ===================================================== */

    public boolean podeSerLicenciado() {
        return Boolean.TRUE.equals(disponivelParaLicenciamento)
                && status == StatusItemAcervo.DISPONIVEL_LICENCIAMENTO
                && tipo != null
                && tipo.podeSerLicenciado();
    }

    public boolean visivelPublicamente() {
        return status != null && status.visivelPublicamente();
    }
}
