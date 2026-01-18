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
 * Gerencia tanto a preservação histórica quanto o licenciamento comercial.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "itens_acervo")
public class ItemAcervo {

    @Id
    private String id;

    /**
     * Título e Contexto Histórico
     */
    private String titulo;
    private String descricao;
    private String local; // Onde o registro foi feito
    private String dataOriginal; // Data histórica (ex: "Junho de 2004" ou "Década de 1920")

    /**
     * Procedência e Propriedade
     */
    private String procedencia; // Ex: "Acervo Pessoal de [Nome da Atleta]" ou "Domínio Público"
    private String fotografoDoador; // Créditos originais

    /**
     * Tipificação e Status de Curadoria
     */
    private TipoItemAcervo tipo; // FOTO, VIDEO, DOCUMENTO, etc.
    private StatusItemAcervo status; // RASCUNHO, PUBLICADO, SOB_ANALISE_JURIDICA, MEMORIAL

    /**
     * Relacionamentos
     */
    private String modalidadeId;
    private List<String> atletasIds; // IDs das atletas vinculadas a este item

    /**
     * Inteligência Financeira e Diferenciação de Frente
     */
    private Boolean disponivelParaLicenciamento; // Se o item pode ser vendido
    private BigDecimal precoBaseLicenciamento; // Valor para uso comercial/editorial

    // Indica se o item é parte da frente Histórica (Pesquisa) ou Ativa (Financeira)
    private Boolean itemHistorico;

    /**
     * Regras de Uso Específicas
     */
    private String restricoesUso;

    /**
     * Arquivos Digitais (Conexão com Cloudinary)
     * Contém as URLs da imagem em baixa (preview) e alta (original)
     */
    private List<FotoAcervo> fotos;

    /**
     * Metadados para Split de Pagamento e Auditoria
     * Guardamos o percentual da atleta no momento da publicação do item
     */
    private BigDecimal percentualRepasseNoMomento;

    private Instant criadoEm;
    private Instant atualizadoEm;
    private String curadorResponsavel;
}