package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import org.springframework.data.annotation.Id;
import lombok.Data;

@Data
@Document(collectionName = "conteudos")
public class Conteudo {

    @Id
    private String id;

    private String titulo;
    private String slug;
    private String conteudoHTML;
}