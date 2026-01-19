package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.ConfiguracaoFiscal;
import br.com.acervodaatletabrasileira.acervoapi.model.Transacao;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConfiguracaoFiscalRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class LicenciamentoService {

    private final ItemAcervoRepository itemRepository;
    private final AtletaRepository atletaRepository;
    private final TransacaoRepository transacaoRepository;
    private final ConfiguracaoFiscalRepository configRepository;

    // Fallback caso o banco esteja sem configuração inicial
    private static final String CONFIG_ID = "GLOBAL_SETTINGS";
    private static final BigDecimal DEFAULT_REPASSE = new BigDecimal("0.85");
    private static final BigDecimal DEFAULT_COMISSAO = new BigDecimal("0.15");

    public LicenciamentoService(ItemAcervoRepository itemRepository,
                                AtletaRepository atletaRepository,
                                TransacaoRepository transacaoRepository,
                                ConfiguracaoFiscalRepository configRepository) {
        this.itemRepository = itemRepository;
        this.atletaRepository = atletaRepository;
        this.transacaoRepository = transacaoRepository;
        this.configRepository = configRepository;
    }

    /**
     * Busca as taxas configuradas ou retorna valores padrão se não existir no banco.
     */
    private Mono<ConfiguracaoFiscal> obterRegrasFiscais() {
        return configRepository.findById(CONFIG_ID)
                .defaultIfEmpty(new ConfiguracaoFiscal(
                        CONFIG_ID,
                        DEFAULT_REPASSE,
                        DEFAULT_COMISSAO,
                        "Configuração padrão do sistema",
                        Instant.now(),
                        "SYSTEM"
                ));
    }

    public Mono<SimulacaoFaturamentoDTO> gerarSimulacaoFaturamento(PropostaLicenciamentoDTO proposta) {
        return obterRegrasFiscais().flatMap(config ->
                itemRepository.findById(proposta.itemAcervoId())
                        .switchIfEmpty(Mono.error(new RuntimeException("Item não encontrado")))
                        .zipWith(atletaRepository.findById(proposta.atletaId()))
                        .map(tuple -> {
                            var item = tuple.getT1();
                            var atleta = tuple.getT2();

                            BigDecimal valorTotal = item.getPrecoBaseLicenciamento() != null ?
                                    item.getPrecoBaseLicenciamento() : BigDecimal.ZERO;

                            // Cálculo usando as regras dinâmicas do banco
                            BigDecimal repasseAtleta = valorTotal.multiply(config.getPercentualRepasseAtleta())
                                    .setScale(2, RoundingMode.HALF_UP);
                            BigDecimal comissaoPlataforma = valorTotal.subtract(repasseAtleta);

                            return new SimulacaoFaturamentoDTO(
                                    item.getTitulo(),
                                    valorTotal,
                                    repasseAtleta,
                                    comissaoPlataforma,
                                    atleta.getChavePix()
                            );
                        })
        );
    }

    public Mono<TransacaoResponseDTO> efetivarLicenciamento(PropostaLicenciamentoDTO proposta) {
        return obterRegrasFiscais().flatMap(config ->
                itemRepository.findById(proposta.itemAcervoId())
                        .zipWith(atletaRepository.findById(proposta.atletaId()))
                        .flatMap(tuple -> {
                            var item = tuple.getT1();
                            var atleta = tuple.getT2();

                            BigDecimal valorTotal = item.getPrecoBaseLicenciamento() != null ?
                                    item.getPrecoBaseLicenciamento() : BigDecimal.ZERO;

                            // Cálculo usando as regras dinâmicas do banco
                            BigDecimal repasseAtleta = valorTotal.multiply(config.getPercentualRepasseAtleta())
                                    .setScale(2, RoundingMode.HALF_UP);
                            BigDecimal comissao = valorTotal.subtract(repasseAtleta);

                            Transacao transacao = new Transacao();
                            transacao.setItemId(item.getId());
                            transacao.setAtletaId(atleta.getId());
                            transacao.setValorBrutoTotal(valorTotal);
                            transacao.setValorLiquidoRepasse(repasseAtleta);
                            transacao.setValorComissaoPlataforma(comissao);

                            // Salva qual era o percentual no momento da venda (Snapshot Fiscal)
                            transacao.setPercentualComissao(config.getPercentualComissaoPlataforma());

                            transacao.setTipoLicenca(proposta.tipoUso());
                            transacao.setMoeda("BRL");
                            transacao.setStatusFinanceiro("CONCLUIDA");
                            transacao.setDataTransacao(Instant.now());
                            transacao.setAtualizadoEm(Instant.now());

                            return transacaoRepository.save(transacao)
                                    .map(this::mapToResponseDTO);
                        })
        );
    }

    public Flux<TransacaoResponseDTO> listarTransacoesPorAtleta(String atletaId) {
        return transacaoRepository.findByAtletaId(atletaId)
                .map(this::mapToResponseDTO);
    }

    public Mono<ExtratoAtletaDTO> gerarExtratoConsolidado(String atletaId) {
        return atletaRepository.findById(atletaId)
                .flatMap(atleta ->
                        transacaoRepository.findByAtletaId(atletaId)
                                .filter(t -> "CONCLUIDA".equals(t.getStatusFinanceiro()))
                                .collectList()
                                .map(lista -> {
                                    BigDecimal saldo = lista.stream()
                                            .map(Transacao::getValorLiquidoRepasse)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .setScale(2, RoundingMode.HALF_UP);

                                    List<TransacaoResponseDTO> historico = lista.stream()
                                            .map(this::mapToResponseDTO)
                                            .toList();

                                    return new ExtratoAtletaDTO(atleta.getNome(), saldo, historico);
                                })
                );
    }

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