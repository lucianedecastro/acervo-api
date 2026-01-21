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
 *
 * É genérico, reutilizável e aplicável a atletas, fotógrafas,
 * instituições ou outros autores.
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
     * (ex: "Cessão de imagem – Atleta X – Jogos 2004")
     */
    private String descricao;

    /* =====================================================
       VÍNCULOS (TODOS OPCIONAIS)
       ===================================================== */

    /**
     * Item de acervo ao qual o documento se refere
     */
    @Indexed
    private String itemAcervoId;

    /**
     * Foto específica (Cloudinary publicId), quando aplicável
     */
    private String fotoPublicId;

    /**
     * Atletas envolvidas no documento
     */
    private List<String> atletasIds;

    /**
     * Autor do material (fotógrafa, agência, instituição etc)
     */
    @Indexed
    private String autorId;

    /**
     * Nome público do autor (exibível no frontend)
     */
    private String autorNomePublico;

    /* =====================================================
       DOCUMENTO DIGITAL
       ===================================================== */

    /**
     * URL ou identificador do PDF assinado no storage
     */
    private String urlDocumento;

    /**
     * Hash do documento para auditoria e integridade
     */
    private String hashDocumento;

    /**
     * Indica se a assinatura digital foi validada
     */
    private Boolean assinaturaDigitalValida;

    /* =====================================================
       REGRAS DE USO E LIMITES
       ===================================================== */

    /**
     * Indica se o documento permite uso comercial
     */
    private Boolean permiteUsoComercial;

    /**
     * Finalidades de uso permitidas
     * (ex: PESQUISA, EDITORIAL, COMERCIAL)
     */
    private List<FinalidadeUso> finalidadesPermitidas;

    /**
     * Territórios nos quais o uso é permitido
     */
    private List<TerritorioUso> territoriosPermitidos;

    /**
     * Restrições adicionais livres
     */
    private String restricoesUso;

    /**
     * Data limite de validade do documento
     */
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

    /**
     * Admin ou responsável jurídico que validou o documento
     */
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

    /**
     * Regra central de autorização para licenciamento
     */
    public boolean permiteLicenciamento() {
        return Boolean.TRUE.equals(permiteUsoComercial)
                && status == StatusDocumentoDireitos.VALIDADO
                && (validoAte == null || validoAte.isAfter(Instant.now()));
    }
}
