package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

@Data
@Document(collectionName = "conteudos")
public class Conteudo {


    private String id;

    private String titulo;
    private String slug;
    private String conteudoHTML;
}