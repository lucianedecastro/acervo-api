// src/main/java/.../model/FotoAcervo.java
package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Construtor vazio para o Firestore
public class FotoAcervo {

    private String url;
    private String legenda;

    // Construtor completo
    public FotoAcervo(String url, String legenda) {
        this.url = url;
        this.legenda = legenda;
    }
}