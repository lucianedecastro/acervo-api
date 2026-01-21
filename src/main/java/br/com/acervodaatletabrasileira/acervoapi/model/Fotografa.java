package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fotografas")
public class Fotografa {

    /* =====================================================
       IDENTIFICAÇÃO
       ===================================================== */

    @Id
    private String id;

    private String nome;
    private String nomeSocial;

    @Indexed(unique = true)
    private String slug;

    @Indexed(unique = true)
    private String cpf;

    @Indexed(unique = true)
    private String email;

    private String senha;

    /**
     * Controle de permissões (Spring Security)
     */
    private String role = "ROLE_FOTOGRAFA";

    /* =====================================================
       PERFIL PÚBLICO
       ===================================================== */

    private String biografia;

    private String registroProfissional;

    private String linkRedeSocialPrincipal;

    /**
     * Até 3 imagens de destaque
     */
    private List<String> fotosDestaque;

    /**
     * Link para a coleção completa da fotógrafa
     */
    private String linkColecaoCompleta;

    private String fotoDestaqueUrl;

    private CategoriaFotografa categoria = CategoriaFotografa.ATIVA;

    /* =====================================================
       DIREITOS AUTORAIS / IMAGEM
       ===================================================== */

    /**
     * Indica se há cessão válida de direitos de imagem das atletas fotografadas
     */
    private Boolean possuiComprovacaoDireitos = false;

    /**
     * PDF assinado digitalmente (upload)
     */
    private String linkDocumentoCessaoDireitos;

    /* =====================================================
       CONTRATO E VERIFICAÇÃO ADMIN
       ===================================================== */

    private Boolean contratoAssinado = false;
    private String linkContratoDigital;
    private Instant dataAssinaturaContrato;

    private StatusVerificacao statusVerificacao = StatusVerificacao.PENDENTE;
    private String observacoesAdmin;
    private Instant dataVerificacao;

    /* =====================================================
       REPRESENTAÇÃO LEGAL (ESPÓLIO)
       ===================================================== */

    private String nomeRepresentante;
    private String cpfRepresentante;
    private String vinculoRepresentante;

    /* =====================================================
       DADOS FINANCEIROS (ÁREA PROTEGIDA)
       ===================================================== */

    private TipoChavePix tipoChavePix;
    private String chavePix;

    private String banco;
    private String agencia;
    private String conta;
    private String tipoConta;

    /**
     * Integração com gateway de pagamento (ex: Stripe, Pagar.me)
     */
    private String gatewayAccountId;

    /**
     * Percentual destinado à fotógrafa
     */
    private BigDecimal percentualRepasse;

    /**
     * Comissão diferenciada negociada com a plataforma
     */
    private BigDecimal comissaoPlataformaDiferenciada;

    /* =====================================================
       STATUS OPERACIONAL
       ===================================================== */

    private StatusFotografa statusFotografa = StatusFotografa.INATIVA;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;
    private Instant atualizadoEm;

    /* =====================================================
       ENUMS
       ===================================================== */

    public enum CategoriaFotografa {
        HISTORICA,
        ATIVA,
        ESPOLIO
    }

    public enum StatusVerificacao {
        PENDENTE,
        VERIFICADO,
        REJEITADO,
        MEMORIAL_PUBLICO
    }

    public enum TipoChavePix {
        CPF,
        EMAIL,
        TELEFONE,
        ALEATORIA,
        NENHUM
    }

    public enum StatusFotografa {
        ATIVA,
        INATIVA,
        BLOQUEADA
    }

    /* =====================================================
       REGRAS DE NEGÓCIO
       ===================================================== */

    /**
     * Só pode gerar receita se:
     * - estiver ativa ou espólio
     * - estiver verificada
     * - contrato assinado
     */
    public boolean podeGerarReceita() {
        return (this.categoria == CategoriaFotografa.ATIVA || this.categoria == CategoriaFotografa.ESPOLIO)
                && this.statusVerificacao == StatusVerificacao.VERIFICADO
                && Boolean.TRUE.equals(this.contratoAssinado);
    }

    /**
     * Indica se a remuneração sofrerá redução por falta de cessão
     */
    public boolean possuiReducaoPorDireitosImagem() {
        return !Boolean.TRUE.equals(this.possuiComprovacaoDireitos);
    }
}
