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
 * IMPORTANTE:
 * - O campo "status" controla apenas o ciclo jurídico do licenciamento.
 * - O registro em Blockchain é tratado separadamente via:
 *   - blockchainTxId
 *   - dataRegistroBlockchain
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

    @Indexed
    private String documentoDireitosId;

    /**
     * Vínculo com a Transacao criada na mesma efetivação.
     * Necessário para checar a liquidação financeira antes
     * de emitir o carimbo institucional (blockchain).
     */
    @Indexed
    private String transacaoId;

    /* =====================================================
       DADOS DO LICENCIADO (REGISTRO INFORMATIVO)
       ===================================================== */

    /**
     * Nome ou Razão Social de quem adquiriu a licença.
     * Salvo como String pois não há módulo de login para compradores.
     */
    private String nomeLicenciado;

    /**
     * CPF ou CNPJ para fins de emissão de certificado/recibo.
     */
    private String documentoIdentificadorLicenciado;

    /* =====================================================
       REGRAS DO USO LICENCIADO
       ===================================================== */

    @Indexed
    private String tipoUso;

    private DocumentoDireitos.TerritorioUso territorio;
    private DocumentoDireitos.FinalidadeUso finalidade;
    private Instant validoAte;

    /* =====================================================
       VALORES DE REFERÊNCIA
       ===================================================== */

    private BigDecimal valorLicenciamento;

    /* =====================================================
       STATUS JURÍDICO E REGISTRO DIGITAL
       ===================================================== */

    /**
     * Status jurídico do licenciamento.
     * Controla apenas o ciclo de vida legal do contrato.
     */
    private StatusLicenciamento status;

    /**
     * Transaction Hash (TxId) gerado pela rede Blockchain.
     * Identificador público da transação que registrou
     * a concessão da licença.
     */
    @Indexed
    private String blockchainTxId;

    /**
     * Data do carimbo imutável na rede.
     */
    private Instant dataRegistroBlockchain;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;

    private Instant atualizadoEm;

    private String aprovadoPor;
}