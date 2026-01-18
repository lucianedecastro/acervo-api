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
     * Slug da modalidade
     * (ex: futebol, atletismo, ginastica)
     * Usado para URLs amigáveis e SEO
     */
    private String slug;

    /**
     * Ícone ou pictograma representativo da modalidade
     * (URL pública – Cloudinary no futuro)
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
     * PublicId da foto de destaque (Cloudinary)
     */
    private String fotoDestaquePublicId;

    /**
     * Indica se a modalidade está ativa no acervo público
     * Permite curadoria sem exclusão
     */
    private Boolean ativa;

    /**
     * Data de criação do registro no acervo
     */
    private Instant criadoEm;

    /**
     * Data da última atualização do registro
     */
    private Instant atualizadoEm;
}