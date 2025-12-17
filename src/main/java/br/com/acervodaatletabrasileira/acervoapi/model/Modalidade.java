package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "modalidades")
public class Modalidade {

    @Id
    private String id;

    /**
     * Nome da modalidade
     * (ex: Futebol, Atletismo, Ginástica)
     */
    private String nome;

    /**
     * Ícone ou pictograma representativo da modalidade
     * (URL pública – pode ser Cloudinary futuramente)
     */
    private String pictogramaUrl;

    /**
     * Texto histórico / descritivo da modalidade
     */
    private String historia;

    /**
     * Imagens históricas associadas à modalidade
     * (subdocumentos do acervo)
     */
    private List<FotoAcervo> fotos;

    /**
     * Identificador da foto de destaque da modalidade
     */
    private String fotoDestaqueId;

    /**
     * Data de criação do registro no acervo
     */
    private Instant criadoEm;
}
