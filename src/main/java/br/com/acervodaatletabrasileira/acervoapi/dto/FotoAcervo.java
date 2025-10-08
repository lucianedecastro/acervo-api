// src/main/java/.../model/FotoAcervo.java

package br.com.acervodaatletabrasileira.acervoapi.dto;

// REMOVIDO: import lombok.Data;
// REMOVIDO: import lombok.NoArgsConstructor;

// CORREÇÃO: Usa 'record' para garantir imutabilidade e concisão
public record FotoAcervo(
        // A URL é o valor do GCS (String)
        String url,

        // A legenda é o texto do frontend
        String legenda
) {
    // Não precisa de corpo, Lombok ou construtores extras!
}
