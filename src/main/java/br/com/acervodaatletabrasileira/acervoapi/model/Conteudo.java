package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

@Data
@Document(collectionName = "conteudos")
public class Conteudo {

    // ✅ CORREÇÃO DEFINITIVA: Seguindo a sua "regra de ouro".
    // Um campo 'id' simples, sem anotações, que será gerenciado pelo seu Service.
    // Exatamente como funciona no seu Atleta.java.
    private String id;

    // Seus campos de dados, mantidos como estavam.
    private String titulo;
    private String slug;
    private String conteudoHTML;
}