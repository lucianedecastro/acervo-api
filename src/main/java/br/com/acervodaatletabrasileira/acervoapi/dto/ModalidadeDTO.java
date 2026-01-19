package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO para criação e atualização de modalidades.
 * Representa os dados necessários para a curadoria das categorias do acervo.
 */
@Schema(description = "Dados para cadastro ou edição de uma modalidade esportiva")
public record ModalidadeDTO(

        @Schema(example = "Natação", description = "Nome da modalidade")
        String nome,

        @Schema(example = "A natação brasileira feminina estreou em Olimpíadas com Maria Lenk em 1932...", description = "Resumo histórico")
        String historia,

        @Schema(example = "https://res.cloudinary.com/...", description = "URL do pictograma/ícone")
        String pictogramaUrl,

        @Schema(example = "true", description = "Define se a modalidade aparece no portal público")
        Boolean ativa,

        @Schema(description = "Lista de fotos históricas associadas à modalidade")
        List<FotoAcervo> fotos,

        @Schema(example = "modalidades/natacao_destaque", description = "PublicId da foto de destaque no Cloudinary")
        String fotoDestaquePublicId
) {
}