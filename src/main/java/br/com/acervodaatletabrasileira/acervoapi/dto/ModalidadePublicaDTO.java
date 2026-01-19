package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import java.util.List;

public record ModalidadePublicaDTO(
        String id,
        String nome,
        String slug,
        String pictogramaUrl,
        String historia,
        List<FotoAcervo> fotos,
        String fotoDestaquePublicId
) {
    public static ModalidadePublicaDTO fromModel(Modalidade modalidade) {
        return new ModalidadePublicaDTO(
                modalidade.getId(),
                modalidade.getNome(),
                modalidade.getSlug(),
                modalidade.getPictogramaUrl(),
                modalidade.getHistoria(),
                modalidade.getFotos(),
                modalidade.getFotoDestaquePublicId()
        );
    }
}
