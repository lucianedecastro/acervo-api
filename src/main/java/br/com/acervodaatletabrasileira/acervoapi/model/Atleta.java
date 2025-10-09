package br.com.acervodaatletabrasileira.acervoapi.model;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoAcervo;
import com.google.cloud.spring.data.firestore.Document;
import org.springframework.data.annotation.Id;  // <-- substitui o import do DocumentId
import lombok.Data;

import java.util.List;

@Data
@Document(collectionName = "atletas")
public class Atleta {

    @Id  // <-- troque de @DocumentId para @Id
    private String id;

    private String nome;
    private String modalidade;
    private String biografia;
    private String competicao;
    private List<FotoAcervo> fotos;
}
