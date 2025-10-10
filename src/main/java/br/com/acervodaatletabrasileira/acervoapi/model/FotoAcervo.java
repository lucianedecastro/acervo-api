package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoAcervo {
    // 🆕 CAMPO NOVO - ID único para cada foto
    private String id;

    // ✅ CAMPOS EXISTENTES (mantidos)
    private String url;     // URL no Google Cloud Storage
    private String legenda; // Legenda da foto

    // 🆕 CAMPO NOVO - controle de destaque
    private Boolean ehDestaque;

    // 🆕 CONSTRUTOR COMPATÍVEL - para não quebrar código existente
    public FotoAcervo(String url, String legenda) {
        this.id = java.util.UUID.randomUUID().toString(); // 🎯 Gera ID automático
        this.url = url;
        this.legenda = legenda;
        this.ehDestaque = false; // Por padrão não é destaque
    }
}