package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoAcervo {
    private String url;     // URL no Google Cloud Storage
    private String legenda; // Legenda da foto
}