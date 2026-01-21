package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.ConfiguracaoFiscal;
import br.com.acervodaatletabrasileira.acervoapi.model.Licenciamento;
import br.com.acervodaatletabrasileira.acervoapi.model.Transacao;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConfiguracaoFiscalRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.LicenciamentoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Serviço de Licenciamento.
 *
 * Responsável por:
 * - simular faturamento
 * - efetivar transações
 * - consultar licenciamentos (admin)
 *
 * NÃO decide autorização jurídica (delegado ao JuridicoService)
 * NÃO executa regras fiscais complexas
 */
@Service
public class LicenciamentoService {

    private final ItemAcervoRepository itemRepository;
    private final AtletaRepository atletaRepository;
    private final TransacaoRepository transacaoRepository;
    private final ConfiguracaoFiscalRepository configRepository;
    private final JuridicoService juridicoService;
    private final LicenciamentoRepository licenciamentoRepository;

    // Fallback caso o banco esteja sem configuração inicial
    private static final String CONFIG_ID = "GLOBAL_SETTINGS";
    private static final BigDecimal DEFAULT_REPASSE = new BigDecimal("0.85");
    private static final BigDecimal DEFAULT_COMISSAO = new BigDecimal("0.15");

    public LicenciamentoService(
            ItemAcervoRepository itemRepository,
            AtletaRepository atletaRepository,
            TransacaoRepository transacaoRepository,
            ConfiguracaoFiscalRepository configRepository,
            JuridicoService juridicoService,
            LicenciamentoRepository licenciamentoRepository
    ) {
        this.itemRepository = itemRepository;
        this.atletaRepository = atletaRepository;
        this.transacaoRepository = transacaoRepository;
        this.configRepository = configRepository;
        this.juridicoService = juridicoService;
        this.licenciamentoRepository = licenciamentoRepository;
    }

    /* =====================================================
       CONFIGURAÇÕES FISCAIS
       ===================================================== */

    private Mono<ConfiguracaoFiscal> obterRegrasFiscais() {
        return configRepository.findById(CONFIG_ID)
                .defaultIfEmpty(
                        new ConfiguracaoFiscal(
                                CONFIG_ID,
                                DEFAULT_REPASSE,
                                DEFAULT_COMISSAO,
                                "Configuração padrão do sistema",
                                Instant.now(),
                                "SYSTEM"
                        )
                );
    }

    /* =====================================================
       SIMULAÇÃO DE FATURAMENTO
       ===================================================== */

    public Mono<SimulacaoFaturamentoDTO> gerarSimulacaoFaturamento(
            PropostaLicenciamentoDTO proposta
    ) {
        return juridicoService
                .podeLicenciarItem(proposta.itemAcervoId())
                .flatMap(podeLicenciar -> {

                    if (!podeLicenciar) {
                        return Mono.error(
                                new IllegalStateException(
                                        "Licenciamento bloqueado por pendência jurídica"
                                )
                        );
                    }

                    return obterRegrasFiscais().flatMap(config ->
                            itemRepository.findById(proposta.itemAcervoId())
                                    .switchIfEmpty(
                                            Mono.error(
                                                    new IllegalArgumentException("Item não encontrado")
                                            )
                                    )
                                    .zipWith(
                                            atletaRepository.findById(proposta.atletaId())
                                    )
                                    .map(tuple -> {

                                        var item = tuple.getT1();
                                        var atleta = tuple.getT2();

                                        BigDecimal valorTotal =
                                                item.getPrecoBaseLicenciamento() != null
                                                        ? item.getPrecoBaseLicenciamento()
                                                        : BigDecimal.ZERO;

                                        BigDecimal repasseAtleta = valorTotal
                                                .multiply(config.getPercentualRepasseAtleta())
                                                .setScale(2, RoundingMode.HALF_UP);

                                        BigDecimal comissaoPlataforma =
                                                valorTotal.subtract(repasseAtleta);

                                        return new SimulacaoFaturamentoDTO(
                                                item.getTitulo(),
                                                valorTotal,
                                                repasseAtleta,
                                                comissaoPlataforma,
                                                atleta.getChavePix()
                                        );
                                    })
                    );
                });
    }

    /* =====================================================
       EFETIVAÇÃO DO LICENCIAMENTO
       ===================================================== */

    public Mono<TransacaoResponseDTO> efetivarLicenciamento(
            PropostaLicenciamentoDTO proposta
    ) {
        return juridicoService
                .podeLicenciarItem(proposta.itemAcervoId())
                .flatMap(podeLicenciar -> {

                    if (!podeLicenciar) {
                        return Mono.error(
                                new IllegalStateException(
                                        "Licenciamento bloqueado por pendência jurídica"
                                )
                        );
                    }

                    return obterRegrasFiscais().flatMap(config ->
                            itemRepository.findById(proposta.itemAcervoId())
                                    .zipWith(
                                            atletaRepository.findById(proposta.atletaId())
                                    )
                                    .flatMap(tuple -> {

                                        var item = tuple.getT1();
                                        var atleta = tuple.getT2();

                                        BigDecimal valorTotal =
                                                item.getPrecoBaseLicenciamento() != null
                                                        ? item.getPrecoBaseLicenciamento()
                                                        : BigDecimal.ZERO;

                                        BigDecimal repasseAtleta = valorTotal
                                                .multiply(config.getPercentualRepasseAtleta())
                                                .setScale(2, RoundingMode.HALF_UP);

                                        BigDecimal comissaoPlataforma =
                                                valorTotal.subtract(repasseAtleta);

                                        Transacao transacao = new Transacao();
                                        transacao.setItemId(item.getId());
                                        transacao.setAtletaId(atleta.getId());
                                        transacao.setValorBrutoTotal(valorTotal);
                                        transacao.setValorLiquidoRepasse(repasseAtleta);
                                        transacao.setValorComissaoPlataforma(comissaoPlataforma);
                                        transacao.setPercentualComissao(
                                                config.getPercentualComissaoPlataforma()
                                        );
                                        transacao.setTipoLicenca(proposta.tipoUso());
                                        transacao.setMoeda("BRL");
                                        transacao.setStatusFinanceiro("CONCLUIDA");
                                        transacao.setDataTransacao(Instant.now());
                                        transacao.setAtualizadoEm(Instant.now());

                                        return transacaoRepository
                                                .save(transacao)
                                                .map(this::mapToResponseDTO);
                                    })
                    );
                });
    }

    /* =====================================================
       CONSULTAS ADMINISTRATIVAS (LICENCIAMENTOS)
       ===================================================== */

    public Flux<Licenciamento> listarTodosLicenciamentos() {
        return licenciamentoRepository.findAll();
    }

    public Flux<Licenciamento> listarLicenciamentosPorItem(String itemAcervoId) {
        return licenciamentoRepository.findByItemAcervoId(itemAcervoId);
    }

    /* =====================================================
       EXTRATOS
       ===================================================== */

    public Flux<TransacaoResponseDTO> listarTransacoesPorAtleta(
            String atletaId
    ) {
        return transacaoRepository
                .findByAtletaId(atletaId)
                .map(this::mapToResponseDTO);
    }

    public Mono<ExtratoAtletaDTO> gerarExtratoConsolidado(
            String atletaId
    ) {
        return atletaRepository.findById(atletaId)
                .switchIfEmpty(
                        Mono.error(
                                new IllegalArgumentException("Atleta não encontrada")
                        )
                )
                .flatMap(atleta ->
                        transacaoRepository.findByAtletaId(atletaId)
                                .filter(t ->
                                        "CONCLUIDA".equals(t.getStatusFinanceiro())
                                )
                                .collectList()
                                .map(lista -> {

                                    BigDecimal saldo = lista.stream()
                                            .map(Transacao::getValorLiquidoRepasse)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .setScale(2, RoundingMode.HALF_UP);

                                    List<TransacaoResponseDTO> historico =
                                            lista.stream()
                                                    .map(this::mapToResponseDTO)
                                                    .toList();

                                    return new ExtratoAtletaDTO(
                                            atleta.getNome(),
                                            saldo,
                                            historico
                                    );
                                })
                );
    }

    /* =====================================================
       MAPEAMENTO
       ===================================================== */

    private TransacaoResponseDTO mapToResponseDTO(Transacao t) {
        return new TransacaoResponseDTO(
                t.getId(),
                t.getItemId(),
                t.getAtletaId(),
                t.getValorBrutoTotal(),
                t.getValorLiquidoRepasse(),
                t.getDataTransacao(),
                t.getStatusFinanceiro(),
                t.getTipoLicenca()
        );
    }
}
