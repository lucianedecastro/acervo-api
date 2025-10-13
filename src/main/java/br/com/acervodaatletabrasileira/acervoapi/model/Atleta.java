package br.com.acervodaatletabrasileira.acervoapi.model;

import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

import java.util.List;

@Data
@Document(collectionName = "atletas")
public class Atleta {

    private String id;


    private String nome;
    private String modalidade;
    private String biografia;
    private String competicao;
    private List<FotoAcervo> fotos;


    private String fotoDestaqueId;

    // ✅ FirestoreDirectService continua gerando o ID automaticamente!
}