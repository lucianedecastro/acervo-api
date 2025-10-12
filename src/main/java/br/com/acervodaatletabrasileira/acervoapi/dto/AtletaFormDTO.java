package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.util.List;


public record AtletaFormDTO(
        String nome,
        String modalidade,
        String biografia,
        String competicao,
        List<FotoDTO> fotos,
        String fotoDestaqueId,
        List<String> fotosRemovidas
) {
}