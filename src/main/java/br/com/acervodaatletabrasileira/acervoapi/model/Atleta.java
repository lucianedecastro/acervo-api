package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "atletas")
public class Atleta {
    @Id
    private String id;
    private String nome;
    private String nomeSocial;
    private List<String> modalidades;
    private String biografia;
    private Boolean contratoAssinado;
    private String linkContratoDigital;
    private Instant dataAssinaturaContrato;

    // Pilares Financeiros
    private String dadosContato;
    private String informacoesParaRepasse; // Usado pelo AtletaService

    // Método auxiliar para o LicenciamentoService
    public String getChavePix() {
        return this.informacoesParaRepasse;
    }

    private BigDecimal percentualRepasse;
    private BigDecimal comissaoPlataformaDiferenciada; // Agora compatível com DTOs

    private List<ItemAcervo> itens;
    private String fotoDestaqueId;
    private Instant criadoEm;
    private Instant atualizadoEm;
    private String statusAtleta;
}