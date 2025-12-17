package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Representa um item do Acervo Carmen Lydia da Mulher Brasileira no Esporte.
 *
 * Um item pode ser:
 * - uma fotografia histórica
 * - um conjunto de imagens
 * - um documento
 * - um recorte jornalístico
 *
 * Este é o modelo persistido no MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "itens_acervo")
public class ItemAcervo {

    @Id
    private String id;

    /**
     * Título principal do item.
     */
    private String titulo;

    /**
     * Texto descritivo / histórico.
     */
    private String descricao;

    /**
     * Tipo do item (FOTO, DOCUMENTO, RECORTE, etc).
     */
    private TipoItemAcervo tipo;

    /**
     * Status editorial do item.
     * Ex: RASCUNHO, PUBLICADO.
     */
    private StatusItemAcervo status;

    /**
     * Modalidade associada ao item.
     * Referência por ID.
     */
    private String modalidadeId;

    /**
     * Atletas relacionadas ao item.
     * Referência por IDs.
     */
    private List<String> atletasIds;

    /**
     * Lista de fotos associadas ao item.
     */
    private List<FotoAcervo> fotos;

    /**
     * Data de criação do item.
     */
    private Instant criadoEm;

    /**
     * Data da última atualização.
     */
    private Instant atualizadoEm;
}
