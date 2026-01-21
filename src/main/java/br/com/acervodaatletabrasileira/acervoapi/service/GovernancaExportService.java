package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.LogDecisao;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de exportação de dados de Governança.
 *
 * Responsável apenas por:
 * - transformar dados em arquivos
 * - NÃO acessa banco
 * - NÃO toma decisões
 */
@Service
public class GovernancaExportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    // BOM UTF-8 para compatibilidade com Excel
    private static final byte[] UTF8_BOM = {
            (byte) 0xEF,
            (byte) 0xBB,
            (byte) 0xBF
    };

    /* =====================================================
       CSV
       ===================================================== */

    public byte[] exportarCsv(List<LogDecisao> logs) {

        // Ordena cronologicamente (auditoria exige ordem temporal)
        List<LogDecisao> ordenados = logs.stream()
                .sorted(Comparator.comparing(LogDecisao::getDataDecisao))
                .toList();

        String header = String.join(";",
                "data_decisao",
                "tipo_decisao",
                "entidade",
                "entidade_id",
                "decisao",
                "justificativa",
                "responsavel",
                "role_responsavel"
        );

        String body = ordenados.stream()
                .map(this::mapToCsvLine)
                .collect(Collectors.joining("\n"));

        byte[] content = (header + "\n" + body)
                .getBytes(StandardCharsets.UTF_8);

        // Aplica BOM UTF-8
        byte[] result = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, result, UTF8_BOM.length, content.length);

        return result;
    }

    private String mapToCsvLine(LogDecisao log) {
        return String.join(";",
                escape(format(log.getDataDecisao())),
                escape(log.getTipoDecisao() != null ? log.getTipoDecisao().name() : null),
                escape(log.getEntidade()),
                escape(log.getEntidadeId()),
                escape(log.getDecisao()),
                escape(log.getJustificativa()),
                escape(log.getResponsavel()),
                escape(log.getRoleResponsavel())
        );
    }

    /* =====================================================
       PDF (RELATÓRIO INSTITUCIONAL)
       ===================================================== */

    /**
     * Gera conteúdo textual estruturado para PDF.
     * (renderização binária fica fora deste serviço)
     */
    public String gerarConteudoPdf(
            List<LogDecisao> logs,
            Instant inicio,
            Instant fim
    ) {

        List<LogDecisao> ordenados = logs.stream()
                .sorted(Comparator.comparing(LogDecisao::getDataDecisao))
                .toList();

        StringBuilder sb = new StringBuilder();

        sb.append("RELATÓRIO DE GOVERNANÇA\n");
        sb.append("Período: ")
                .append(inicio != null ? format(inicio) : "início")
                .append(" até ")
                .append(fim != null ? format(fim) : "atual")
                .append("\n");

        sb.append("Total de registros: ")
                .append(ordenados.size())
                .append("\n");

        sb.append("Gerado em: ")
                .append(format(Instant.now()))
                .append("\n\n");

        for (LogDecisao log : ordenados) {
            sb.append("Data: ").append(format(log.getDataDecisao())).append("\n");
            sb.append("Tipo: ").append(log.getTipoDecisao()).append("\n");
            sb.append("Entidade: ").append(log.getEntidade())
                    .append(" (").append(log.getEntidadeId()).append(")\n");
            sb.append("Decisão: ").append(log.getDecisao()).append("\n");
            sb.append("Justificativa: ").append(log.getJustificativa()).append("\n");
            sb.append("Responsável: ")
                    .append(log.getResponsavel())
                    .append(" - ")
                    .append(log.getRoleResponsavel())
                    .append("\n");
            sb.append("----------------------------------------\n");
        }

        return sb.toString();
    }

    /* =====================================================
       UTIL
       ===================================================== */

    private String format(Instant instant) {
        return instant != null ? DATE_FORMAT.format(instant) : "";
    }

    /**
     * Escape padrão CSV (RFC):
     * - envolve em aspas
     * - duplica aspas internas
     * - remove quebras de linha
     */
    private String escape(String value) {
        if (value == null) return "\"\"";
        String sanitized = value
                .replace("\"", "\"\"")
                .replace("\n", " ")
                .replace("\r", " ");
        return "\"" + sanitized + "\"";
    }
}
