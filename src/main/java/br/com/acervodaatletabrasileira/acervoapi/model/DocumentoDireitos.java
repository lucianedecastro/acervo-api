package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Documento jurídico que comprova cessão ou autorização
 * de direitos autorais e/ou direitos de imagem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documentos_direitos")
public class DocumentoDireitos {

    @Id
    private String id;

    /* =====================================================
       IDENTIFICAÇÃO DO DOCUMENTO
       ===================================================== */

    private TipoDocumentoDireitos tipoDocumento;

    /**
     * Descrição resumida do documento
     */
    private String descricao;

    /* =====================================================
       VÍNCULOS (TODOS OPCIONAIS)
       ===================================================== */

    @Indexed
    private String itemAcervoId;

    private String fotoPublicId;

    private List<String> atletasIds;

    @Indexed
    private String autorId;

    private String autorNomePublico;

    /* =====================================================
       DOCUMENTO DIGITAL
       ===================================================== */

    private String urlDocumento;

    /**
     * Hash do documento para auditoria e integridade.
     * Mapeado para findByHashConteudo no Repository.
     */
    @Indexed
    private String hashConteudo;

    private Boolean assinaturaDigitalValida;

    /* =====================================================
       AUDITORIA E INTEGRIDADE (INCREMENTO)
       ===================================================== */

    /**
     * Registro da validação jurídica na Blockchain.
     */
    @Indexed
    private String blockchainTxId;

    /**
     * Data em que o selo de imutabilidade foi gerado.
     */
    private Instant dataRegistroBlockchain;

    /* =====================================================
       REGRAS DE USO E LIMITES
       ===================================================== */

    private Boolean permiteUsoComercial;

    private List<FinalidadeUso> finalidadesPermitidas;

    private List<TerritorioUso> territoriosPermitidos;

    private String restricoesUso;

    @Indexed
    private Instant validoAte;

    /* =====================================================
       STATUS JURÍDICO
       ===================================================== */

    @Indexed
    private StatusDocumentoDireitos status;

    private String observacoesJuridico;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;
    private Instant atualizadoEm;

    private String responsavelValidacao;

    /* =====================================================
       ENUMS
       ===================================================== */

    public enum TipoDocumentoDireitos {
        CESSAO_DIREITOS_AUTORAIS,
        CESSAO_DIREITOS_IMAGEM,
        AUTORIZACAO_USO_IMAGEM,
        DOMINIO_PUBLICO,
        OUTRO
    }

    public enum StatusDocumentoDireitos {
        PENDENTE_ANALISE,
        VALIDADO,
        REJEITADO,
        EXPIRADO
    }

    public enum FinalidadeUso {
        PESQUISA,
        INSTITUCIONAL,
        EDITORIAL,
        COMERCIAL,
        PUBLICITARIO
    }

    public enum TerritorioUso {
        BRASIL,
        INTERNACIONAL,
        ESPECIFICO
    }

    /* =====================================================
       REGRAS DE NEGÓCIO
       ===================================================== */

    public boolean permiteLicenciamento() {
        return Boolean.TRUE.equals(permiteUsoComercial)
                && status == StatusDocumentoDireitos.VALIDADO
                && (validoAte == null || validoAte.isAfter(Instant.now()));
    }
}