package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.*;
import br.com.acervodaatletabrasileira.acervoapi.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Licenciamento.
 *
 * RESPONSABILIDADES:
 * - Validar possibilidade jurídica
 * - Calcular split financeiro (simulação)
 * - Criar registro de Transação (estado inicial)
 * - Criar registro formal de Licenciamento
 * - Acionar a cobrança real no gateway (Asaas), com split automático
 *
 * IMPORTANTE:
 * - NÃO registra automaticamente na Blockchain.
 * - A confirmação de liquidação financeira chega via webhook do Asaas
 *   (ver AsaasWebhookService), não é confirmada de forma síncrona aqui.
 * - Registro institucional ocorre via Governança/Admin.
 */
@Service
@Slf4j
public class LicenciamentoService {

    private final ItemAcervoRepository itemRepository;
    private final AtletaRepository atletaRepository;
    private final TransacaoRepository transacaoRepository;
    private final ConfiguracaoFiscalRepository configRepository;
    private final JuridicoService juridicoService;
    private final LicenciamentoRepository licenciamentoRepository;
    private final AsaasService asaasService;
    private final BlockchainService blockchainService;
    private final GovernancaService governancaService;

    private static final String CONFIG_ID = "GLOBAL_SETTINGS";
    private static final BigDecimal DEFAULT_REPASSE = new BigDecimal("0.85");
    private static final BigDecimal DEFAULT_COMISSAO = new BigDecimal("0.15");

    public LicenciamentoService(
            ItemAcervoRepository itemRepository,
            AtletaRepository atletaRepository,
            TransacaoRepository transacaoRepository,
            ConfiguracaoFiscalRepository configRepository,
            JuridicoService juridicoService,
            LicenciamentoRepository licenciamentoRepository,
            AsaasService asaasService,
            BlockchainService blockchainService,
            GovernancaService governancaService
    ) {
        this.itemRepository = itemRepository;
        this.atletaRepository = atletaRepository;
        this.transacaoRepository = transacaoRepository;
        this.configRepository = configRepository;
        this.juridicoService = juridicoService;
        this.licenciamentoRepository = licenciamentoRepository;
        this.asaasService = asaasService;
        this.blockchainService = blockchainService;
        this.governancaService = governancaService;
    }

    /* =====================================================
       REGRAS FISCAIS
       ===================================================== */

    private Mono<ConfiguracaoFiscal> obterRegrasFiscais() {
        return configRepository.findById(CONFIG_ID)
                .defaultIfEmpty(
                        new ConfiguracaoFiscal(
                                CONFIG_ID,
                                DEFAULT_REPASSE,
                                DEFAULT_COMISSAO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                "Configuração padrão do sistema",
                                Instant.now(),
                                "SYSTEM"
                        )
                );
    }

    /* =====================================================
       SIMULAÇÃO
       ===================================================== */

    public Mono<SimulacaoFaturamentoDTO> gerarSimulacaoFaturamento(PropostaLicenciamentoDTO proposta) {

        return juridicoService.podeLicenciarItem(proposta.itemAcervoId())
                .flatMap(podeLicenciar -> {

                    if (!podeLicenciar) {
                        return Mono.error(new IllegalStateException("Licenciamento bloqueado por pendência jurídica"));
                    }

                    return obterRegrasFiscais().flatMap(config ->
                            itemRepository.findById(proposta.itemAcervoId())
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                                    .zipWith(atletaRepository.findById(proposta.atletaId()))
                                    .map(tuple -> {

                                        ItemAcervo item = tuple.getT1();
                                        Atleta atleta = tuple.getT2();

                                        if (!item.podeSerLicenciado()) {
                                            throw new IllegalStateException("Item não disponível para licenciamento");
                                        }

                                        BigDecimal valorTotal = item.getPrecoBaseLicenciamento() != null
                                                ? item.getPrecoBaseLicenciamento()
                                                : BigDecimal.ZERO;

                                        BigDecimal percRepasse = (atleta.getPercentualRepasse() != null)
                                                ? atleta.getPercentualRepasse()
                                                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                                                : config.getPercentualRepasseAtleta();

                                        BigDecimal repasseAtleta = valorTotal
                                                .multiply(percRepasse)
                                                .setScale(2, RoundingMode.HALF_UP);

                                        BigDecimal comissaoPlataforma = valorTotal.subtract(repasseAtleta);

                                        String chaveDestino = (atleta.getIndicacaoEspolio() != null &&
                                                atleta.getIndicacaoEspolio().getChavePixHerdeiro() != null)
                                                ? atleta.getIndicacaoEspolio().getChavePixHerdeiro()
                                                : atleta.getChavePix();

                                        return new SimulacaoFaturamentoDTO(
                                                item.getTitulo(),
                                                valorTotal,
                                                repasseAtleta,
                                                comissaoPlataforma,
                                                chaveDestino
                                        );
                                    })
                    );
                });
    }

    /* =====================================================
       EFETIVAÇÃO (ATO FORMAL – SEM LIQUIDAÇÃO FINANCEIRA)
       ===================================================== */

    public Mono<TransacaoResponseDTO> efetivarLicenciamento(PropostaLicenciamentoDTO proposta) {

        return juridicoService.podeLicenciarItem(proposta.itemAcervoId())
                .flatMap(podeLicenciar -> {

                    if (!podeLicenciar) {
                        return Mono.error(new IllegalStateException("Licenciamento bloqueado por pendência jurídica"));
                    }

                    return obterRegrasFiscais().flatMap(config ->
                            itemRepository.findById(proposta.itemAcervoId())
                                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                                    .zipWith(atletaRepository.findById(proposta.atletaId()))
                                    .flatMap(tuple -> {

                                        ItemAcervo item = tuple.getT1();
                                        Atleta atleta = tuple.getT2();

                                        if (!item.podeSerLicenciado()) {
                                            return Mono.error(new IllegalStateException("Item não disponível para licenciamento"));
                                        }

                                        if (atleta.getGatewayAccountId() == null || atleta.getGatewayAccountId().isBlank()) {
                                            return Mono.error(new IllegalStateException(
                                                    "Atleta ainda não possui conta de recebimento configurada no gateway de pagamentos"
                                            ));
                                        }

                                        BigDecimal valorTotal = item.getPrecoBaseLicenciamento() != null
                                                ? item.getPrecoBaseLicenciamento()
                                                : BigDecimal.ZERO;

                                        Transacao transacao = criarObjetoTransacao(
                                                item,
                                                atleta,
                                                config,
                                                valorTotal,
                                                proposta.tipoUso()
                                        );

                                        Licenciamento licenca = new Licenciamento();
                                        licenca.setItemAcervoId(item.getId());
                                        licenca.setAtletaId(atleta.getId());
                                        licenca.setTipoUso(proposta.tipoUso());
                                        licenca.setValorLicenciamento(valorTotal);
                                        licenca.setStatus(StatusLicenciamento.SOLICITADO);
                                        licenca.setCriadoEm(Instant.now());
                                        licenca.setAtualizadoEm(Instant.now());

                                        // Salva o estado inicial (PENDENTE_GATEWAY) antes de
                                        // chamar o Asaas: a Transacao precisa de um ID próprio
                                        // para servir de externalReference na cobrança.
                                        return transacaoRepository.save(transacao)
                                                .flatMap(tSalva -> {
                                                    licenca.setTransacaoId(tSalva.getId());
                                                    return licenciamentoRepository.save(licenca)
                                                            .then(processarPagamentoGateway(tSalva, atleta, proposta))
                                                            .flatMap(transacaoRepository::save);
                                                })
                                                .map(this::mapToResponseDTO);
                                    })
                    );
                });
    }

    /* =====================================================
       INTEGRAÇÃO COM O GATEWAY (ASAAS)
       ===================================================== */

    /**
     * Cria o cliente (comprador) e a cobrança com split no Asaas.
     *
     * Em caso de falha no gateway, a Transacao NÃO é perdida:
     * fica marcada como "ERRO_GATEWAY" para retentativa manual/futura
     * (ainda não há job de retry automático).
     */
    private Mono<Transacao> processarPagamentoGateway(
            Transacao transacaoSalva,
            Atleta atleta,
            PropostaLicenciamentoDTO proposta
    ) {
        Map<String, Object> dadosCliente = new HashMap<>();
        dadosCliente.put("name", proposta.nomeLicenciado() != null
                ? proposta.nomeLicenciado()
                : "Licenciado não identificado");
        dadosCliente.put("cpfCnpj", proposta.documentoIdentificadorLicenciado());

        return asaasService.criarCliente(dadosCliente)
                .flatMap(cliente -> {

                    String customerId = (String) cliente.get("id");

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("customer", customerId);
                    payload.put("billingType", "UNDEFINED"); // comprador escolhe Pix/boleto/cartão no checkout
                    payload.put("value", transacaoSalva.getValorBrutoTotal());
                    payload.put("dueDate", LocalDate.now().plusDays(3).toString());
                    payload.put("description", "Licenciamento de uso - Acervo Carmen Lydia");
                    payload.put("externalReference", transacaoSalva.getId());
                    payload.put("split", List.of(Map.of(
                            "walletId", atleta.getGatewayAccountId(),
                            "fixedValue", transacaoSalva.getValorLiquidoRepasse()
                    )));

                    return asaasService.criarCobrancaComSplit(payload)
                            .map(cobranca -> {
                                transacaoSalva.setGatewayCustomerId(customerId);
                                transacaoSalva.setGatewayId((String) cobranca.get("id"));
                                transacaoSalva.setLinkPagamento((String) cobranca.get("invoiceUrl"));
                                transacaoSalva.setAtualizadoEm(Instant.now());
                                return transacaoSalva;
                            });
                })
                .onErrorResume(erro -> {
                    log.error("Falha ao criar cobrança no Asaas para a transação {}", transacaoSalva.getId(), erro);
                    transacaoSalva.setStatusFinanceiro("ERRO_GATEWAY");
                    transacaoSalva.setAtualizadoEm(Instant.now());
                    return Mono.just(transacaoSalva);
                });
    }

    /* =====================================================
       CARIMBO INSTITUCIONAL (BLOCKCHAIN) – PÓS-LIQUIDAÇÃO
       ===================================================== */

    /**
     * Registra a prova institucional na Blockchain para um Licenciamento
     * já liquidado financeiramente.
     *
     * Ato administrativo separado, deliberadamente manual:
     * - Exige que a Transacao vinculada esteja "LIQUIDADA".
     * - É o momento em que o Licenciamento passa de SOLICITADO para APROVADO.
     * - O mesmo TxId é gravado em Licenciamento e Transacao, pois ambos
     *   representam o mesmo evento jurídico-financeiro.
     */
    public Mono<Licenciamento> registrarSeloInstitucional(String licenciamentoId, String responsavel) {

        return licenciamentoRepository.findById(licenciamentoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Licenciamento não encontrado")))
                .flatMap(licenciamento -> {

                    if (licenciamento.getBlockchainTxId() != null && !licenciamento.getBlockchainTxId().isBlank()) {
                        return Mono.error(new IllegalStateException(
                                "Licenciamento já possui selo institucional registrado"
                        ));
                    }

                    if (licenciamento.getTransacaoId() == null || licenciamento.getTransacaoId().isBlank()) {
                        return Mono.error(new IllegalStateException(
                                "Licenciamento sem transação financeira vinculada"
                        ));
                    }

                    return transacaoRepository.findById(licenciamento.getTransacaoId())
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Transação vinculada não encontrada")))
                            .flatMap(transacao -> {

                                if (!"LIQUIDADA".equals(transacao.getStatusFinanceiro())) {
                                    return Mono.error(new IllegalStateException(
                                            "Carimbo institucional só pode ser emitido após a liquidação financeira (status atual: "
                                                    + transacao.getStatusFinanceiro() + ")"
                                    ));
                                }

                                String hashRegistro = gerarHashRegistro(licenciamento, transacao);

                                return blockchainService.registrarProvaInstitucional(
                                                "LICENCIAMENTO", licenciamento.getId(), hashRegistro
                                        )
                                        .flatMap(txHash -> {

                                            Instant agora = Instant.now();

                                            licenciamento.setBlockchainTxId(txHash);
                                            licenciamento.setDataRegistroBlockchain(agora);
                                            licenciamento.setStatus(StatusLicenciamento.APROVADO);
                                            licenciamento.setAprovadoPor(responsavel);
                                            licenciamento.setAtualizadoEm(agora);

                                            transacao.setBlockchainTxId(txHash);
                                            transacao.setDataRegistroBlockchain(agora);
                                            transacao.setStatusRegistroInstitucional(StatusRegistroInstitucional.REGISTRADO_COM_SUCESSO);
                                            transacao.setAtualizadoEm(agora);

                                            return transacaoRepository.save(transacao)
                                                    .then(licenciamentoRepository.save(licenciamento));
                                        })
                                        .flatMap(licSalvo ->
                                                governancaService.registrarDecisao(
                                                        TipoDecisao.LICENCIAMENTO,
                                                        "LICENCIAMENTO",
                                                        licSalvo.getId(),
                                                        "SELO_INSTITUCIONAL_REGISTRADO",
                                                        "Licenciamento aprovado e registrado na blockchain. TxId: " + licSalvo.getBlockchainTxId(),
                                                        responsavel,
                                                        "ROLE_ADMIN"
                                                ).thenReturn(licSalvo)
                                        )
                                        .onErrorResume(erro -> {
                                            log.error("Falha ao registrar selo institucional do licenciamento {}", licenciamentoId, erro);
                                            transacao.setStatusRegistroInstitucional(StatusRegistroInstitucional.FALHA_NO_REGISTRO);
                                            transacao.setAtualizadoEm(Instant.now());
                                            return transacaoRepository.save(transacao).then(Mono.error(erro));
                                        });
                            });
                });
    }

    /**
     * Hash canônico do registro (não é hash de arquivo, é hash dos fatos
     * jurídico-financeiros imutáveis do licenciamento + transação).
     * O BlockchainService usa esse valor como "hashArquivo" ao montar
     * o hash consolidado final (entidade + id + hash + timestamp).
     */
    private String gerarHashRegistro(Licenciamento licenciamento, Transacao transacao) {

        String estrutura =
                "itemAcervoId=" + licenciamento.getItemAcervoId() +
                        "|atletaId=" + licenciamento.getAtletaId() +
                        "|tipoUso=" + licenciamento.getTipoUso() +
                        "|valorLicenciamento=" + licenciamento.getValorLicenciamento() +
                        "|transacaoId=" + transacao.getId() +
                        "|valorBrutoTotal=" + transacao.getValorBrutoTotal() +
                        "|valorLiquidoRepasse=" + transacao.getValorLiquidoRepasse() +
                        "|gatewayId=" + transacao.getGatewayId();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(estrutura.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash do registro de licenciamento", e);
        }
    }

    /* =====================================================
       CRIAÇÃO DA TRANSAÇÃO (ESTADO INICIAL)
       ===================================================== */

    private Transacao criarObjetoTransacao(
            ItemAcervo item,
            Atleta atleta,
            ConfiguracaoFiscal config,
            BigDecimal valorTotal,
            String tipoUso
    ) {

        BigDecimal percRepasse = (atleta.getPercentualRepasse() != null)
                ? atleta.getPercentualRepasse()
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                : config.getPercentualRepasseAtleta();

        BigDecimal repasseAtleta = valorTotal
                .multiply(percRepasse)
                .setScale(2, RoundingMode.HALF_UP);

        Transacao t = new Transacao();
        t.setItemId(item.getId());
        t.setAtletaId(atleta.getId());
        t.setValorBrutoTotal(valorTotal);
        t.setValorLiquidoRepasse(repasseAtleta);
        t.setValorComissaoPlataforma(valorTotal.subtract(repasseAtleta));
        t.setPercentualComissao(config.getPercentualComissaoPlataforma());
        t.setTipoLicenca(tipoUso);
        t.setMoeda("BRL");

        // Estado inicial financeiro
        t.setStatusFinanceiro("PENDENTE_GATEWAY");

        // Prova institucional ainda não registrada
        t.setStatusRegistroInstitucional(StatusRegistroInstitucional.NAO_REGISTRADO);

        t.setDataTransacao(Instant.now());
        t.setAtualizadoEm(Instant.now());

        return t;
    }

    /* =====================================================
       CONSULTAS
       ===================================================== */

    public Flux<Licenciamento> listarTodosLicenciamentos() {
        return licenciamentoRepository.findAll();
    }

    public Flux<Licenciamento> listarLicenciamentosPorItem(String itemAcervoId) {
        return licenciamentoRepository.findByItemAcervoId(itemAcervoId);
    }

    public Flux<TransacaoResponseDTO> listarTransacoesPorAtleta(String atletaId) {
        return transacaoRepository.findByAtletaId(atletaId)
                .map(this::mapToResponseDTO);
    }

    /**
     * Lista todas as transações, sem filtro por atleta.
     * Uso exclusivo da visão administrativa geral (AdminVendas).
     */
    public Flux<TransacaoResponseDTO> listarTodasTransacoes() {
        return transacaoRepository.findAll()
                .map(this::mapToResponseDTO);
    }

    public Mono<ExtratoAtletaDTO> gerarExtratoConsolidado(String atletaId) {

        return atletaRepository.findById(atletaId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta ->
                        transacaoRepository.findByAtletaId(atletaId)
                                .filter(t -> "LIQUIDADA".equals(t.getStatusFinanceiro()))
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

    private TransacaoResponseDTO mapToResponseDTO(Transacao t) {
        return new TransacaoResponseDTO(
                t.getId(),
                t.getItemId(),
                t.getAtletaId(),
                t.getValorBrutoTotal(),
                t.getValorLiquidoRepasse(),
                t.getDataTransacao(),
                t.getStatusFinanceiro(),
                t.getTipoLicenca(),
                t.getLinkPagamento()
        );
    }
}