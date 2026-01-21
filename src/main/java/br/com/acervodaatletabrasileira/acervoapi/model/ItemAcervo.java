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
     * (nome livre, não depende de entidade interna)
     */
    private String creditoAutoral;

    /**
     * Identificador interno opcional do autor
     * (pode apontar para fotógrafa, agência ou outro módulo no futuro)
     */
    private String autorId;

    /* =====================================================
       TIPOLOGIA E STATUS
       ===================================================== */

    private TipoItemAcervo tipo;      // FOTO, VIDEO, DOCUMENTO, etc
    private StatusItemAcervo status;  // RASCUNHO, PUBLICADO, DISPONIVEL_LICENCIAMENTO, MEMORIAL...

    /* =====================================================
       RELACIONAMENTOS
       ===================================================== */

    private String modalidadeId;

    /**
     * Atletas relacionadas ao item (pode ser vazio)
     */
    private List<String> atletasIds;

    /* =====================================================
       CONTROLE DE LICENCIAMENTO
       ===================================================== */

    /**
     * Flag editorial (controle rápido)
     */
    private Boolean disponivelParaLicenciamento;

    /**
     * Preço base de licenciamento
     * (pode ser recalculado no checkout)
     */
    private BigDecimal precoBaseLicenciamento;

    /**
     * Indica se o item pertence à frente histórica (pesquisa)
     * ou ativa (comercial)
     */
    private Boolean itemHistorico;

    /**
     * Restrições específicas de uso
     * (ex: "Somente editorial", "Proibido uso comercial")
     */
    private String restricoesUso;

    /* =====================================================
       DIREITOS AUTORAIS / IMAGEM (NÃO OBRIGATÓRIO)
       ===================================================== */

    /**
     * Indica se existe documentação jurídica válida
     * (cessão de direitos de imagem/autoral)
     */
    private Boolean possuiDocumentacaoDireitos;

    /**
     * Referência ao documento jurídico (PDF, hash, storageId)
     */
    private String documentoDireitosId;

    /* =====================================================
       ARQUIVOS DIGITAIS
       ===================================================== */

    /**
     * Arquivos associados ao item (fotos, frames, documentos digitalizados)
     */
    private List<FotoAcervo> fotos;

    /* =====================================================
       INTELIGÊNCIA FINANCEIRA / AUDITORIA
       ===================================================== */

    /**
     * Percentual de repasse congelado no momento da publicação/licenciamento
     */
    private BigDecimal percentualRepasseNoMomento;

    /**
     * Identificador de lote de pagamento (split, gateway, auditoria)
     */
    private String loteFinanceiroId;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;
    private Instant atualizadoEm;

    /**
     * Curador responsável pela validação editorial/jurídica
     */
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
