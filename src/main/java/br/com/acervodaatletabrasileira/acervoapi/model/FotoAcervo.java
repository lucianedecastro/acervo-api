package br.com.acervodaatletabrasileira.acervoapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoAcervo {
    private String id;
    private String url;
    private String legenda;


    @JsonProperty("ehDestaque")
    private Boolean ehDestaque;

    public FotoAcervo(String url, String legenda) {
        this.id = java.util.UUID.randomUUID().toString();
        this.url = url;
        this.legenda = legenda;
        this.ehDestaque = false;
    }
}