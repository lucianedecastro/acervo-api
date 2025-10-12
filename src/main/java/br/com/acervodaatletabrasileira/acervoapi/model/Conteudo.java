package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import org.springframework.data.annotation.Id; // ✅ IMPORTAÇÃO CORRETA E FINAL
import lombok.Data;

@Data
@Document(collectionName = "conteudos")
public class Conteudo {

    // ✅ CORREÇÃO DEFINITIVA: Usamos a anotação @Id do Spring Data.
    // Isso resolve a confusão do Spring. Ele agora sabe que o campo 'slug'
    // é o identificador oficial desta classe, assim como o campo 'id' é
    // o identificador por convenção da classe Atleta.
    @Id
    private String slug;

    private String titulo;
    private String conteudoHTML;
}