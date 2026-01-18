package br.com.acervodaatletabrasileira.acervoapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para criação e atualização de modalidades.
 * Representa os dados básicos necessários para a curadoria das categorias do acervo.
 */
@Schema(description = "Dados para cadastro de uma nova modalidade esportiva")
public record ModalidadeDTO(

        @Schema(example = "Natação", description = "Nome da modalidade")
        String nome,

        @Schema(example = "A natação brasileira feminina estreou em Olimpíadas com Maria Lenk em 1932...", description = "Resumo histórico")
        String historia,

        @Schema(example = "https://res.cloudinary.com/...", description = "URL do pictograma/ícone")
        String pictogramaUrl,

        @Schema(example = "true", description = "Define se a modalidade aparece no portal público")
        Boolean ativa
) {
}