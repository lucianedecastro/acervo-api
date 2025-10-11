package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;

@Data
@Document(collectionName = "modalidades")
public class Modalidade {

    // Usaremos a mesma estratégia de ID do Atleta:
    // um UUID em string que será gerenciado pelo nosso serviço.
    private String id;

    private String nome;
    private String pictogramaUrl;
    private String historia; // Armazenará o HTML do RichTextEditor
}