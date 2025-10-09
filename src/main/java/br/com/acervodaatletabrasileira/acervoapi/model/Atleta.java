package br.com.acervodaatletabrasileira.acervoapi.model;

import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

import java.util.List;

@Data
@Document(collectionName = "atletas")
public class Atleta {

    // 🚨 REMOVE o campo id - Firestore vai gerar automaticamente!
    // private String id; ← EXCLUÍDO

    private String nome;
    private String modalidade;
    private String biografia;
    private String competicao;
    private List<FotoAcervo> fotos;
}