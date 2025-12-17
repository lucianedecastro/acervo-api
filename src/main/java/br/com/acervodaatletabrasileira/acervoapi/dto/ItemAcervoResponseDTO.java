package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.time.Instant;
import java.util.List;

/**
 * DTO de resposta pública para itens do Acervo Carmen Lydia.
 *
 * Usado em:
 * - listagens públicas
 * - páginas de detalhe
 *
 * NÃO deve expor informações internas ou administrativas.
 */
public record ItemAcervoResponseDTO(

        /**
         * Identificador público do item do acervo.
         */
        String id,

        /**
         * Título principal do item.
         */
        String titulo,

        /**
         * Texto histórico / descritivo.
         */
        String descricao,

        /**
         * Tipo do item (ex: FOTO, DOCUMENTO, RECORTE).
         */
        TipoItemAcervo tipo,

        /**
         * Status editorial.
         * Apenas itens PUBLICADOS devem ser retornados publicamente.
         */
        StatusItemAcervo status,

        /**
         * Modalidade associada.
         */
        String modalidadeId,

        /**
         * Atletas relacionadas ao item.
         */
        List<String> atletasIds,

        /**
         * Fotos públicas do item.
         */
        List<FotoDTO> fotos,

        /**
         * Data de criação do item.
         */
        Instant criadoEm,

        /**
         * Data da última atualização.
         */
        Instant atualizadoEm
) {
}

