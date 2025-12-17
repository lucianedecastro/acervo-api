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
@Document(collection = "atletas")
public class Atleta {

    @Id
    private String id;

    /**
     * Nome completo da atleta
     */
    private String nome;

    /**
     * Modalidade principal da atleta
     * (ex: Futebol, Atletismo, Judô)
     */
    private String modalidade;

    /**
     * Texto biográfico / histórico
     */
    private String biografia;

    /**
     * Competição, contexto ou marco histórico associado
     * (ex: Olimpíadas 1996, Campeonato Paulista, etc.)
     */
    private String competicao;

    /**
     * Conjunto de fotos do acervo histórico
     */
    private List<FotoAcervo> fotos;

    /**
     * Identificador da foto de destaque (publicId do Cloudinary)
     */
    private String fotoDestaqueId;

    /**
     * Data de criação do registro no acervo
     */
    private Instant criadoEm;
}
