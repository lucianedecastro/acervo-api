package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta.CategoriaAtleta;

public record AtletaRegistroDTO(
        String nome,
        String email,
        String senha,
        String cpf,
        String slug,
        CategoriaAtleta categoria
) {}
