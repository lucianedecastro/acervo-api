package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document; // MUDANÇA APLICADA AQUI
import lombok.Data;

@Data
@Document(collectionName = "atletas")
public class Atleta {

    @DocumentId
    private String id;

    private String nome;
    private String modalidade;
    private String biografia;
    private String imagemUrl;
    private String competicao;
}