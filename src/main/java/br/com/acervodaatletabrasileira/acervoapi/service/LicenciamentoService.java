package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Transacao;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class LicenciamentoService {

    private final ItemAcervoRepository itemRepository;
    private final AtletaRepository atletaRepository;
    private final TransacaoRepository transacaoRepository;

    public LicenciamentoService(ItemAcervoRepository itemRepository,
                                AtletaRepository atletaRepository,
                                TransacaoRepository transacaoRepository) {
        this.itemRepository = itemRepository;
        this.atletaRepository = atletaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public Mono<SimulacaoFaturamentoDTO> gerarSimulacaoFaturamento(PropostaLicenciamentoDTO proposta) {
        return itemRepository.findById(proposta.itemAcervoId())
                .zipWith(atletaRepository.findById(proposta.atletaId()))
                .map(tuple -> {
                    var item = tuple.getT1();
                    var atleta = tuple.getT2();

                    BigDecimal valorTotal = item.getPrecoBaseLicenciamento();
                    BigDecimal percentualAtleta = new BigDecimal("0.85");
                    BigDecimal repasseAtleta = valorTotal.multiply(percentualAtleta);
                    BigDecimal comissaoPlataforma = valorTotal.subtract(repasseAtleta);

                    return new SimulacaoFaturamentoDTO(
                            item.getTitulo(),
                            valorTotal,
                            repasseAtleta,
                            comissaoPlataforma,
                            atleta.getChavePix()
                    );
                });
    }

    public Mono<TransacaoResponseDTO> efetivarLicenciamento(PropostaLicenciamentoDTO proposta) {
        return itemRepository.findById(proposta.itemAcervoId())
                .zipWith(atletaRepository.findById(proposta.atletaId()))
                .flatMap(tuple -> {
                    var item = tuple.getT1();
                    var atleta = tuple.getT2();

                    BigDecimal valorTotal = item.getPrecoBaseLicenciamento();
                    BigDecimal percentualAtleta = new BigDecimal("0.85");
                    BigDecimal repasseAtleta = valorTotal.multiply(percentualAtleta);
                    BigDecimal comissao = valorTotal.subtract(repasseAtleta);

                    Transacao transacao = new Transacao();
                    transacao.setItemId(item.getId());
                    transacao.setAtletaId(atleta.getId());
                    transacao.setValorBrutoTotal(valorTotal);
                    transacao.setValorLiquidoRepasse(repasseAtleta);
                    transacao.setValorComissaoPlataforma(comissao);
                    transacao.setPercentualComissao(new BigDecimal("0.15"));
                    transacao.setTipoLicenca(proposta.tipoUso());
                    transacao.setMoeda("BRL");
                    transacao.setStatusFinanceiro("CONCLUIDA");
                    transacao.setDataTransacao(Instant.now());

                    return transacaoRepository.save(transacao)
                            .map(t -> new TransacaoResponseDTO(
                                    t.getId(),
                                    t.getItemId(),
                                    t.getAtletaId(),
                                    t.getValorBrutoTotal(),
                                    t.getValorLiquidoRepasse(),
                                    t.getDataTransacao(),
                                    t.getStatusFinanceiro(),
                                    t.getTipoLicenca() // <--- CAMPO ADICIONADO AQUI
                            ));
                });
    }

    public Flux<TransacaoResponseDTO> listarTransacoesPorAtleta(String atletaId) {
        return transacaoRepository.findByAtletaId(atletaId)
                .map(t -> new TransacaoResponseDTO(
                        t.getId(),
                        t.getItemId(),
                        t.getAtletaId(),
                        t.getValorBrutoTotal(),
                        t.getValorLiquidoRepasse(),
                        t.getDataTransacao(),
                        t.getStatusFinanceiro(),
                        t.getTipoLicenca() // <--- CAMPO ADICIONADO AQUI
                ));
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
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    List<TransacaoResponseDTO> historico = lista.stream()
                                            .map(t -> new TransacaoResponseDTO(
                                                    t.getId(), t.getItemId(), t.getAtletaId(),
                                                    t.getValorBrutoTotal(), t.getValorLiquidoRepasse(),
                                                    t.getDataTransacao(), t.getStatusFinanceiro(),
                                                    t.getTipoLicenca() // <--- CAMPO ADICIONADO AQUI
                                            )).toList();

                                    return new ExtratoAtletaDTO(atleta.getNome(), saldo, historico);
                                })
                );
    }
}