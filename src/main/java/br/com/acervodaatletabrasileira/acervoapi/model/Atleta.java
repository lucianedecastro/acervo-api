package br.com.acervodaatletabrasileira.acervoapi.model;

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
    private String slug; // Ex: maria-lenk (gerado automaticamente)

    @Indexed(unique = true)
    private String cpf;

    @Indexed(unique = true)
    private String email;

    // --- Campo vital para o login (Necessário para UserDetailsServiceImpl) ---
    private String senha;

    private List<String> modalidadesIds; // IDs das modalidades (Natação, etc)
    private String biografia;

    // --- Diferenciação de Frente (Histórico vs Financeiro) ---
    private CategoriaAtleta categoria = CategoriaAtleta.ATIVA;

    // --- Gestão de Contrato e Verificação ---
    private Boolean contratoAssinado = false;
    private String linkContratoDigital;
    private Instant dataAssinaturaContrato;

    private StatusVerificacao statusVerificacao = StatusVerificacao.PENDENTE;
    private String observacoesAdmin;
    private Instant dataVerificacao;

    // --- Representação Legal (Para Espólio ou Menores) ---
    private String nomeRepresentante;
    private String cpfRepresentante;
    private String vinculoRepresentante; // Ex: Herdeiro, Mãe/Pai, Procurador

    // --- Pilares Financeiros Estruturados ---
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

    private String fotoDestaqueUrl; // Foto de perfil principal
    private String statusAtleta; // Ex: ATIVO, INATIVO, MEMORIAL

    private Instant criadoEm;
    private Instant atualizadoEm;

    // --- Enums para Regras de Negócio ---
    public enum CategoriaAtleta {
        HISTORICA,  // Apenas pesquisa, sem licenciamento ativo
        ATIVA,      // Atleta viva que gere o próprio perfil
        ESPOLIO     // Atleta falecida com herdeiros gerindo o licenciamento
    }

    public enum StatusVerificacao {
        PENDENTE, VERIFICADO, REJEITADO, MEMORIAL_PUBLICO
    }

    public enum TipoChavePix {
        CPF, EMAIL, TELEFONE, ALEATORIA, NENHUM
    }

    // Lógica para saber se este perfil pode gerar receita
    public boolean podeGerarReceita() {
        return (this.categoria == CategoriaAtleta.ATIVA || this.categoria == CategoriaAtleta.ESPOLIO)
                && this.statusVerificacao == StatusVerificacao.VERIFICADO;
    }
}