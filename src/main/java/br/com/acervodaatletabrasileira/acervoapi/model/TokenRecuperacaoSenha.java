package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Pedido de redefinição de senha. Guarda apenas o HASH do token: quem ler o banco
 * não consegue usar o link. A expiração é verificada no código (os índices
 * automáticos do MongoDB não estão ligados neste projeto).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tokens_recuperacao_senha")
public class TokenRecuperacaoSenha {

    public enum TipoConta { ADMIN, ATLETA, FOTOGRAFA }

    @Id
    private String id;

    private String tokenHash;

    /** E-mail exatamente como está gravado na conta. */
    private String email;

    private TipoConta tipoConta;

    private Instant criadoEm;
    private Instant expiraEm;
}
