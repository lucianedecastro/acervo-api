package br.com.acervodaatletabrasileira.acervoapi.dto;

public record ModalidadeDTO(

        /**
         * Nome da modalidade
         * (ex: Futebol, Atletismo)
         */
        String nome,

        /**
         * Texto histórico / descritivo
         */
        String historia,

        /**
         * URL do pictograma da modalidade
         */
        String pictogramaUrl,

        /**
         * Define se a modalidade está ativa no acervo público
         */
        Boolean ativa
) {
}
