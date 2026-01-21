package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Registro imutável de decisões relevantes para
 * governança, auditoria e compliance da plataforma.
 *
 * NÃO altera estado do sistema.
 * Apenas registra o que foi decidido.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "logs_decisoes")
public class LogDecisao {

    @Id
    private String id;

    /* =====================================================
       CONTEXTO DA DECISÃO
       ===================================================== */

    /**
     * Tipo da decisão (Jurídica, Fiscal, Licenciamento etc)
     */
    @Indexed
    private TipoDecisao tipoDecisao;

    /**
     * Entidade afetada pela decisão
     * (ex: DOCUMENTO_DIREITOS, ITEM_ACERVO, TRANSACAO)
     */
    private String entidade;

    /**
     * ID da entidade afetada
     */
    @Indexed
    private String entidadeId;

    /**
     * Descrição objetiva da decisão
     */
    private String decisao;

    /**
     * Justificativa ou observação detalhada
     */
    private String justificativa;

    /* =====================================================
       RESPONSABILIDADE
       ===================================================== */

    /**
     * Quem tomou a decisão (admin, sistema, jurídico)
     */
    @Indexed
    private String responsavel;

    /**
     * Papel de quem decidiu (ROLE_ADMIN, SYSTEM, etc)
     */
    private String roleResponsavel;

    /* =====================================================
       AUDITORIA
       ===================================================== */

    private Instant dataDecisao;
}

