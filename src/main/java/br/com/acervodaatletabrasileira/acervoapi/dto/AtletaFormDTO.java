package br.com.acervodaatletabrasileira.acervoapi.dto;

import java.util.List;

public record AtletaFormDTO(

        String nome,

        String modalidade,
        // pode ser o nome da modalidade ou o id (decidimos depois)

        String biografia,

        String competicao,
        // competições, eventos ou contexto histórico principal

        List<FotoDTO> fotos,
        // novas fotos enviadas ou fotos existentes mantidas

        String fotoDestaqueId,
        // id da foto marcada como destaque no frontend

        List<String> fotosRemovidas
        // ids das fotos removidas pelo admin
) {
}
