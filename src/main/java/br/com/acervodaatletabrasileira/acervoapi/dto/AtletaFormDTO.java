package br.com.acervodaatletabrasileira.acervoapi.dto;

import org.springframework.web.multipart.MultipartFile;


public record AtletaFormDTO(

        String nome,
        String modalidade,
        String biografia,
        String competicao,


        MultipartFile file,
        String legenda
) {

}
