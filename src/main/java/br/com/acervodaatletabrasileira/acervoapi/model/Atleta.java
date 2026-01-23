package br.com.acervodaatletabrasileira.acervoapi.model;

import br.com.acervodaatletabrasileira.acervoapi.model.FotoPerfilAtleta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.math.BigDecimal;

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
     * Essencial para o Spring Security diferenciar as permissões.
     * Para atletas, o valor padrão deve ser ROLE_ATLETA.
     */
    private String role = "ROLE_ATLETA";

    private List<String> modalidadesIds;
    private String biografia;

    private CategoriaAtleta categoria = CategoriaAtleta.ATIVA;

    private Boolean contratoAssinado = false;
    private String linkContratoDigital;
    private Instant dataAssinaturaContrato;

    private StatusVerificacao statusVerificacao = StatusVerificacao.PENDENTE;
    private String observacoesAdmin;
    private Instant dataVerificacao;

    private String nomeRepresentante;
    private String cpfRepresentante;
    private String vinculoRepresentante;

    private String dadosContato;
    private TipoChavePix tipoChavePix;
    private String chavePix;
    private String banco;
    private String agencia;
    private String conta;
    private String tipoConta;
    private String gatewayAccountId;

    private BigDecimal percentualRepasse;
    private BigDecimal comissaoPlataformaDiferenciada;

    private FotoPerfilAtleta fotoPerfil;
    private String fotoDestaqueUrl;
    private String statusAtleta;

    private Instant criadoEm;
    private Instant atualizadoEm;

    // --- Enums ---
    public enum CategoriaAtleta {
        HISTORICA, ATIVA, ESPOLIO
    }

    public enum StatusVerificacao {
        PENDENTE, VERIFICADO, REJEITADO, MEMORIAL_PUBLICO
    }

    public enum TipoChavePix {
        CPF, EMAIL, TELEFONE, ALEATORIA, NENHUM
    }

    public boolean podeGerarReceita() {
        return (this.categoria == CategoriaAtleta.ATIVA || this.categoria == CategoriaAtleta.ESPOLIO)
                && this.statusVerificacao == StatusVerificacao.VERIFICADO;
    }
}