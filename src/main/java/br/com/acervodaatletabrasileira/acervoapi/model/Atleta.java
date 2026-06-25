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

/**
 * Representa uma atleta vinculada ao Acervo da Mulher Brasileira no Esporte.
 *
 * IMPORTANTE:
 * - A plataforma NÃO utiliza wallets ou identidade cripto individual.
 * - A Blockchain é utilizada apenas como camada institucional de governança.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "atletas")
public class Atleta {

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
     * Papel de segurança no sistema.
     * Atletas devem possuir ROLE_ATLETA.
     */
    private String role = "ROLE_ATLETA";

    private List<String> modalidadesIds;
    private String biografia;

    private CategoriaAtleta categoria = CategoriaAtleta.ATIVA;

    /* =====================================================
       GESTÃO JURÍDICA E CONTRATUAL
       ===================================================== */

    private Boolean contratoAssinado = false;
    private String linkContratoDigital;
    private String contratoGestaoHash;
    private Instant dataAssinaturaContrato;
    private Boolean permissaoUploadTerceiros = false;

    /* =====================================================
       VALIDAÇÃO DE IDENTIDADE (KYC / HISTÓRICO)
       ===================================================== */

    private StatusVerificacao statusVerificacao = StatusVerificacao.PENDENTE;
    private String observacoesAdmin;
    private Instant dataVerificacao;

    /**
     * Documento de identidade ou comprovação histórica.
     */
    private String documentoIdentidadeUrl;

    /**
     * Resultado de validação automática (quando houver integração).
     */
    private Boolean identidadeValidadaApi;

    /**
     * Score ou observação retornada por API de verificação.
     */
    private String scoreValidacaoDocumento;

    /* =====================================================
       PESQUISA HISTÓRICA
       ===================================================== */

    private String fontePesquisa;
    private String linkFontePesquisa;

    /* =====================================================
       REPRESENTAÇÃO LEGAL
       ===================================================== */

    private String nomeRepresentante;
    private String cpfRepresentante;
    private String vinculoRepresentante;

    /* =====================================================
       SUCESSÃO / ESPÓLIO
       ===================================================== */

    private DadosEspolio indicacaoEspolio;

    /* =====================================================
       DADOS FINANCEIROS
       ===================================================== */

    private String dadosContato;

    private TipoChavePix tipoChavePix;
    private String chavePix;

    private String banco;
    private String agencia;
    private String conta;
    private String tipoConta;

    /**
     * walletId da subconta no Gateway (Asaas).
     * É esse valor (não o accountId) que entra no array "split"
     * das cobranças, para o repasse automático à atleta.
     */
    private String gatewayAccountId;

    private BigDecimal percentualRepasse;
    private BigDecimal comissaoPlataformaDiferenciada;

    /* =====================================================
       IMAGENS DE PERFIL
       ===================================================== */

    private FotoPerfilAtleta fotoPerfil;
    private FotoPerfilAtleta fotoDestaque;

    /**
     * Campo legado para compatibilidade.
     */
    private String fotoDestaqueUrl;

    private String statusAtleta;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant criadoEm;
    private Instant atualizadoEm;

    /* =====================================================
       CLASSES INTERNAS
       ===================================================== */

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosEspolio {
        private String nomeHerdeiro;
        private String cpfHerdeiro;
        private String emailContato;
        private String chavePixHerdeiro;
        private String grauParentesco;
    }

    /* =====================================================
       ENUMS
       ===================================================== */

    public enum CategoriaAtleta {
        HISTORICA, ATIVA, ESPOLIO
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

    /* =====================================================
       REGRAS DE NEGÓCIO
       ===================================================== */

    /**
     * Indica se a atleta pode gerar receita.
     *
     * Requisitos:
     * - Categoria ATIVA ou ESPÓLIO
     * - Identidade verificada
     */
    public boolean podeGerarReceita() {
        return (this.categoria == CategoriaAtleta.ATIVA
                || this.categoria == CategoriaAtleta.ESPOLIO)
                && this.statusVerificacao == StatusVerificacao.VERIFICADO;
    }
}