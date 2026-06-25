package br.com.acervodaatletabrasileira.acervoapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * =====================================================
 * BLOCKCHAIN SERVICE — CAMADA INSTITUCIONAL DE GOVERNANÇA
 * =====================================================
 *
 * FINALIDADE:
 * - Registrar provas institucionais de integridade.
 * - Não executa regra de negócio.
 * - Não realiza split financeiro.
 * - Não interage com dados sensíveis.
 *
 * CONCEITO:
 * A Blockchain é utilizada como:
 * → Camada externa de imutabilidade institucional.
 * → Selo público de integridade.
 *
 * PREPARADO PARA:
 * - Evolução futura para Smart Contract.
 * - Registro consolidado com metadados institucionais.
 */
@Service
@Slf4j
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final RawTransactionManager transactionManager;

    private final long chainId = 80002; // Polygon Amoy Testnet

    public BlockchainService(
            @Value("${blockchain.alchemy-url:}") String alchemyUrl,
            @Value("${blockchain.private-key:}") String privateKey
    ) {
        Web3j web3jTemp = null;
        Credentials credentialsTemp = null;
        RawTransactionManager transactionManagerTemp = null;

        if (alchemyUrl.isBlank() || privateKey.isBlank()) {
            log.warn("BlockchainService inicializado sem 'blockchain.alchemy-url'/'blockchain.private-key' " +
                    "configurados. Chamadas de registro institucional vão falhar até a configuração ser definida.");
        } else {
            try {
                web3jTemp = Web3j.build(new HttpService(alchemyUrl));
                credentialsTemp = Credentials.create(privateKey);
                transactionManagerTemp = new RawTransactionManager(web3jTemp, credentialsTemp, chainId);
            } catch (Exception e) {
                // Não deixa NENHUM problema de configuração (chave malformada,
                // URL inválida, etc.) derrubar o boot da aplicação. Mesmo
                // princípio do CloudinaryConfig/AsaasService, só que aqui
                // cobrindo "valor presente mas inválido", não só "valor ausente".
                log.warn("BlockchainService: falha ao inicializar com a configuração fornecida ({}). " +
                                "Chamadas de registro institucional vão falhar até a configuração ser corrigida.",
                        e.getMessage());
                web3jTemp = null;
                credentialsTemp = null;
                transactionManagerTemp = null;
            }
        }

        this.web3j = web3jTemp;
        this.credentials = credentialsTemp;
        this.transactionManager = transactionManagerTemp;
    }

    /* =====================================================
       REGISTRO INSTITUCIONAL CONSOLIDADO
       ===================================================== */

    /**
     * Registra prova institucional consolidada na Blockchain.
     *
     * FLUXO:
     * 1️⃣ Gera hash consolidado (arquivo + metadados institucionais)
     * 2️⃣ Envia transação real assinada
     * 3️⃣ Aguarda confirmação
     *
     * IMPORTANTE:
     * - Não inclui dados pessoais.
     * - Não inclui dados financeiros.
     * - Apenas IDs internos e hash técnico.
     *
     * @param entidade        Ex: DOCUMENTO_DIREITOS, ITEM_ACERVO, LICENCIAMENTO
     * @param entidadeId      ID interno da entidade
     * @param hashArquivo     SHA-256 do arquivo original
     * @return TxHash confirmado na rede
     */
    public Mono<String> registrarProvaInstitucional(
            String entidade,
            String entidadeId,
            String hashArquivo
    ) {

        if (transactionManager == null) {
            return Mono.error(new IllegalStateException(
                    "BlockchainService não está configurado (blockchain.alchemy-url / blockchain.private-key ausentes)"
            ));
        }

        return Mono.fromCallable(() -> {

            String hashConsolidado = gerarHashConsolidado(
                    entidade,
                    entidadeId,
                    hashArquivo
            );

            log.info("Registrando prova institucional. Entidade: {} | ID: {}",
                    entidade, entidadeId);

            String dataHex = Numeric.toHexString(
                    hashConsolidado.getBytes(StandardCharsets.UTF_8)
            );

            var response = transactionManager.sendTransaction(
                    DefaultGasProvider.GAS_PRICE,
                    DefaultGasProvider.GAS_LIMIT,
                    credentials.getAddress(),
                    dataHex,
                    BigInteger.ZERO
            );

            if (response.hasError()) {
                throw new RuntimeException("Erro ao enviar transação: " +
                        response.getError().getMessage());
            }

            String txHash = response.getTransactionHash();

            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                    .send()
                    .getTransactionReceipt()
                    .orElseThrow(() ->
                            new RuntimeException("Transação ainda não minerada."));

            if (!receipt.isStatusOK()) {
                throw new RuntimeException("Transação falhou na rede.");
            }

            log.info("Prova institucional registrada com sucesso. TxHash: {}", txHash);

            return txHash;

        }).subscribeOn(Schedulers.boundedElastic());
    }

    /* =====================================================
       GERAÇÃO DE HASH CONSOLIDADO
       ===================================================== */

    /**
     * Gera hash consolidado com:
     * - Tipo da entidade
     * - ID interno
     * - Hash técnico do arquivo
     * - Timestamp institucional
     *
     * NÃO inclui dados sensíveis.
     */
    private String gerarHashConsolidado(
            String entidade,
            String entidadeId,
            String hashArquivo
    ) throws Exception {

        String estruturaOrdenada =
                "entidade=" + entidade +
                        "|id=" + entidadeId +
                        "|hashArquivo=" + hashArquivo +
                        "|timestamp=" + Instant.now().toString();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(
                estruturaOrdenada.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    /* =====================================================
       IDENTIDADE DIGITAL (LEGADO ESTRATÉGICO)
       ===================================================== */

    /**
     * MÉTODO MANTIDO APENAS COMO REFERÊNCIA ESTRATÉGICA.
     *
     * NÃO UTILIZADO NO FLUXO ATUAL.
     *
     * Mantido para eventual:
     * - Identidade digital institucional futura
     * - Evolução para modelos tokenizados
     *
     * Atualmente a plataforma NÃO utiliza wallets
     * nem exige interação cripto das atletas.
     */
    /*
    public Mono<CarteiraGerada> gerarNovaCarteiraAtleta() {

        return Mono.fromCallable(() -> {

            var ecKeyPair = Keys.createEcKeyPair();
            Credentials credsAtleta = Credentials.create(ecKeyPair);

            String enderecoPublico = credsAtleta.getAddress();
            String chavePrivada = ecKeyPair.getPrivateKey().toString(16);

            log.info("Nova carteira gerada. Endereço: {}", enderecoPublico);

            return new CarteiraGerada(enderecoPublico, chavePrivada);

        }).subscribeOn(Schedulers.boundedElastic());
    }
    */

    /*
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CarteiraGerada {
        private String enderecoPublico;
        private String chavePrivada;
    }
    */
}