package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AdminDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.Transacao;
import br.com.acervodaatletabrasileira.acervoapi.model.ConfiguracaoFiscal;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConfiguracaoFiscalRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AtletaRepository atletaRepository;
    private final ItemAcervoRepository itemRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final TransacaoRepository transacaoRepository;
    private final ConfiguracaoFiscalRepository configRepository;

    private static final String CONFIG_ID = "GLOBAL_SETTINGS";

    public DashboardService(AtletaRepository atletaRepository,
                            ItemAcervoRepository itemRepository,
                            ModalidadeRepository modalidadeRepository,
                            TransacaoRepository transacaoRepository,
                            ConfiguracaoFiscalRepository configRepository) {
        this.atletaRepository = atletaRepository;
        this.itemRepository = itemRepository;
        this.modalidadeRepository = modalidadeRepository;
        this.transacaoRepository = transacaoRepository;
        this.configRepository = configRepository;
    }

    /**
     * Visão Consolidada para Curadoria e Gestão (ADMIN)
     * Agora integra as taxas dinâmicas para o cálculo de comissões.
     */
    public Mono<AdminDashboardStatsDTO> getAdminStats() {
        return configRepository.findById(CONFIG_ID)
                .defaultIfEmpty(new ConfiguracaoFiscal(CONFIG_ID, new BigDecimal("0.85"), new BigDecimal("0.15"), "Padrão", Instant.now(), "SYSTEM"))
                .flatMap(config -> Mono.zip(
                        atletaRepository.count(),
                        itemRepository.count(),
                        modalidadeRepository.count(),
                        itemRepository.countByStatus(StatusItemAcervo.RASCUNHO),
                        itemRepository.findAll().collectList(),
                        transacaoRepository.findAll().collectList()
                ).map(tuple -> {
                    var itensPorTipo = tuple.getT5().stream()
                            .filter(item -> item.getTipo() != null)
                            .collect(Collectors.groupingBy(
                                    item -> item.getTipo().name(),
                                    Collectors.counting()
                            ));

                    BigDecimal faturamentoTotal = tuple.getT6().stream()
                            .map(Transacao::getValorBrutoTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    // Em vez de taxa fixa, usamos o que o Admin definiu na Tabela Fiscal
                    BigDecimal totalComissoes = faturamentoTotal.multiply(config.getPercentualComissaoPlataforma())
                            .setScale(2, RoundingMode.HALF_UP);

                    return new AdminDashboardStatsDTO(
                            tuple.getT1(), // totalAtletas
                            tuple.getT2(), // totalItens
                            tuple.getT3(), // totalModalidades
                            tuple.getT4(), // itensAguardandoPublicacao
                            itensPorTipo,
                            faturamentoTotal,
                            totalComissoes
                    );
                }));
    }

    /**
     * Visão Personalizada para a Atleta (Finanças e Acervo)
     */
    public Mono<AtletaDashboardStatsDTO> getAtletaStats(String identificador) {
        return atletaRepository.findById(identificador)
                .switchIfEmpty(atletaRepository.findByEmail(identificador))
                .switchIfEmpty(Mono.error(new RuntimeException("Atleta não encontrada: " + identificador)))
                .flatMap(atleta -> {
                    String atletaId = atleta.getId();
                    return Mono.zip(
                            itemRepository.countByAtletasIdsContaining(atletaId),
                            itemRepository.countByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.PUBLICADO),
                            itemRepository.countByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.RASCUNHO),
                            itemRepository.countByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.MEMORIAL),
                            transacaoRepository.findByAtletaId(atletaId).collectList()
                    ).map(tuple -> {
                        // Aqui somamos o que já foi calculado no momento da venda (Snapshot)
                        BigDecimal saldoTotalAtleta = tuple.getT5().stream()
                                .map(Transacao::getValorLiquidoRepasse)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);

                        return new AtletaDashboardStatsDTO(
                                tuple.getT1(),
                                tuple.getT2(),
                                tuple.getT3(),
                                tuple.getT4(),
                                (long) tuple.getT5().size(),
                                saldoTotalAtleta
                        );
                    });
                });
    }
}