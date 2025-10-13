package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

@Data
@Document(collectionName = "modalidades")
public class Modalidade {


    private String id;

    private String nome;
    private String pictogramaUrl;
    private String historia;
}