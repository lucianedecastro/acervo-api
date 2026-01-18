package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import java.util.List;

public record AtletaPerfilDTO(
        AtletaPublicoDTO atleta, // Agora usamos o DTO limpo aqui
        List<ItemAcervo> itens
) {}