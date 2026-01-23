package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoPerfilAtleta {

    /**
     * publicId no Cloudinary
     */
    private String publicId;

    /**
     * URL pública da foto de perfil
     */
    private String url;

    /**
     * Nome original do arquivo
     */
    private String nomeArquivo;

    /**
     * Data do upload
     */
    private Instant criadaEm;

    /**
     * Controle simples de ativação
     */
    private Boolean ativa = true;
}

