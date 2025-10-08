// src/main/java/.../model/Atleta.java (ATUALIZADO)

package br.com.acervodaatletabrasileira.acervoapi.model;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoAcervo;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

import java.util.List;

@Data
@Document(collectionName = "atletas")
public class Atleta {

    @DocumentId
    private String id;

    private String nome;
    private String modalidade;
    private String biografia;
    private String competicao;


    private List<FotoAcervo> fotos;

}