package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.util.List;

/**
 * DTO de criação/edição de itens do Acervo Carmen Lydia.
 *
 * Usado EXCLUSIVAMENTE por administradores.
 * Representa o que pode ser enviado pelo frontend.
 */
public record ItemAcervoCreateDTO(

        /**
         * Título principal do item do acervo.
         * Ex: "Marta na Copa de 2007"
         */
        String titulo,

        /**
         * Texto descritivo / histórico do item.
         */
        String descricao,

        /**
         * Tipo do item (ex: FOTO, DOCUMENTO, RECORTE, etc).
         */
        TipoItemAcervo tipo,

        /**
         * Status editorial do item.
         * Ex: RASCUNHO, PUBLICADO
         */
        StatusItemAcervo status,

        /**
         * Modalidade associada (id da modalidade).
         */
        String modalidadeId,

        /**
         * Atletas relacionadas ao item (ids).
         * Permite um item com múltiplas atletas.
         */
        List<String> atletasIds,

        /**
         * Lista de fotos associadas ao item.
         */
        List<FotoDTO> fotos
) {
}

