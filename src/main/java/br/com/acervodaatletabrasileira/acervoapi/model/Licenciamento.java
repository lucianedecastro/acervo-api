package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Representa o ato formal de licenciamento de um item do acervo.
 *
 * É a ponte entre:
 * - o Jurídico (autorização)
 * - o Financeiro (transação)
 *
 * NÃO representa pagamento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "licenciamentos")
public class Licenciamento {

    @Id
    private String id;

    /* =====================================================
       VÍNCULOS DE DOMÍNIO
       ===================================================== */

    @Indexed
    private String itemAcervoId;

    @Indexed
    private String atletaId;

    /**
     * Documento jurídico que fundamenta este licenciamento
     */
    @Indexed
    private String documentoDireitosId;

    /* =====================================================
       REGRAS DO USO LICENCIADO
       ===================================================== */

    /**
     * Tipo de uso autorizado
     * (EDITORIAL, COMERCIAL, PUBLICITARIO etc)
     */
    private String tipoUso;

    /**
     * Território autorizado
     */
    private DocumentoDireitos.TerritorioUso territorio;

    /**
     * Finalidade principal do licenciamento
     */
    private DocumentoDireitos.FinalidadeUso finalidade;

    /**
     * Prazo de validade do licenciamento
     */
    private Instant validoAte;

    /* =====================================================
       VALORES DE REFERÊNCIA
       ===================================================== */

    /**
     * Valor acordado para o licenciamento
     * (base para cálculo fiscal)
     */
    private BigDecimal valorLicenciamento;

    /* =====================================================
       STATUS DO LICENCIAMENTO
       ===================================================== */

    private StatusLicenciamento status;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;
    private Instant atualizadoEm;

    /**
     * Quem aprovou o licenciamento (admin/jurídico)
     */
    private String aprovadoPor;
}

