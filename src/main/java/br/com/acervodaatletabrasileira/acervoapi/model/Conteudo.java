package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;

@Data
@Document(collectionName = "conteudos")
public class Conteudo {

    // O slug será nosso ID principal no Firestore. Ex: "historia-acervo"
    @DocumentId
    private String slug;

    private String titulo;      // Ex: "História do Acervo (Página Antessala)"
    private String conteudoHTML; // O texto formatado vindo do RichTextEditor
}
