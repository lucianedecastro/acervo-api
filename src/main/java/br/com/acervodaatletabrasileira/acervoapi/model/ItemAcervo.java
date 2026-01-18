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
 * Representa um ativo do Acervo Digital Carmen Lydia.
 * Focado na gestão, preservação e licenciamento de acervos pessoais.
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
    private String dataOriginal; // Data histórica (ex: "Junho de 2004")

    /**
     * Procedência (Pilar Central da Nova Proposta)
     * Identifica que o material pertence ao acervo privado da atleta.
     */
    private String procedencia; // Ex: "Acervo Pessoal de [Nome da Atleta]"
    private String fotografoDoador; // Créditos originais se conhecidos

    /**
     * Tipificação e Status
     */
    private TipoItemAcervo tipo; // FOTO, VIDEO, DOCUMENTO, CREDENCIAL, RECORTE
    private StatusItemAcervo status; // RASCUNHO, PUBLICADO, SOB_ANALISE_JURIDICA

    /**
     * Relacionamentos
     */
    private String modalidadeId;
    private List<String> atletasIds; // Atletas que aparecem ou são citadas no item

    /**
     * Inteligência Financeira e Licenciamento
     */
    private BigDecimal precoBaseLicenciamento; // Valor para uso comercial/editorial
    private Boolean disponivelParaLicenciamento; // Se a atleta autorizou a circulação remunerada

    /**
     * Regras de Uso Específicas
     * Ex: "Apenas para fins acadêmicos", "Uso proibido em campanhas políticas"
     */
    private String restricoesUso;

    /**
     * Arquivos Digitais (Cloudinary)
     */
    private List<FotoAcervo> fotos;

    /**
     * Auditoria e Curadoria
     */
    private Instant criadoEm;
    private Instant atualizadoEm;
    private String curadorResponsavel; // Nome de quem catalogou o item
}