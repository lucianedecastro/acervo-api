package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.TokenRecuperacaoSenha;
import br.com.acervodaatletabrasileira.acervoapi.model.TokenRecuperacaoSenha.TipoConta;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.FotografaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.TokenRecuperacaoSenhaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Recuperação de senha para admins, atletas e fotógrafas.
 *
 * - O pedido SEMPRE parece bem-sucedido, exista ou não a conta (não revela cadastros).
 * - O link vale 1 hora e só pode ser usado uma vez.
 * - Limites: 1 pedido por e-mail a cada 60 s e teto global por hora (protege a cota do Resend).
 */
@Service
@Slf4j
public class RecuperacaoSenhaService {

    static final Duration VALIDADE = Duration.ofHours(1);
    static final Duration INTERVALO_POR_EMAIL = Duration.ofSeconds(60);
    static final int LIMITE_GLOBAL_POR_HORA = 60;
    static final String MENSAGEM_LINK_INVALIDO = "Link inválido ou expirado. Peça uma nova redefinição de senha.";

    private final TokenRecuperacaoSenhaRepository tokens;
    private final UsuarioAdminRepository admins;
    private final AtletaRepository atletas;
    private final FotografaRepository fotografas;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final String frontendUrl;

    private final SecureRandom random = new SecureRandom();
    private final AtomicLong janelaInicio = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger enviosNaJanela = new AtomicInteger();

    private record Conta(TipoConta tipo, String email) {}

    public RecuperacaoSenhaService(
            TokenRecuperacaoSenhaRepository tokens,
            UsuarioAdminRepository admins,
            AtletaRepository atletas,
            FotografaRepository fotografas,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.tokens = tokens;
        this.admins = admins;
        this.atletas = atletas;
        this.fotografas = fotografas;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /* ==========================
       1. PEDIDO
       ========================== */

    /**
     * Dispara o processamento em segundo plano e retorna na hora, para que o tempo de
     * resposta não permita descobrir se o e-mail existe.
     */
    public Mono<Void> solicitar(String emailDigitado) {
        processarSolicitacao(emailDigitado)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, e -> log.error("Erro ao processar recuperação de senha", e));
        return Mono.empty();
    }

    Mono<Void> processarSolicitacao(String emailDigitado) {
        String digitado = emailDigitado.trim();

        return buscarConta(digitado)
                .switchIfEmpty(Mono.defer(() -> buscarConta(digitado.toLowerCase(Locale.ROOT))))
                .flatMap(this::gerarEEnviar)
                .then();
    }

    private Mono<Void> gerarEEnviar(Conta conta) {
        Instant agora = Instant.now();

        return tokens.deleteByExpiraEmBefore(agora)
                .then(tokens.findFirstByEmailOrderByCriadoEmDesc(conta.email())
                        .map(ultimo -> ultimo.getCriadoEm().plus(INTERVALO_POR_EMAIL).isAfter(agora))
                        .defaultIfEmpty(false))
                .flatMap(muitoCedo -> {
                    if (muitoCedo) {
                        log.info("Recuperação de senha ignorada: pedido repetido em menos de 60 s");
                        return Mono.<Void>empty();
                    }
                    if (!dentroDoLimiteGlobal()) {
                        log.warn("Recuperação de senha ignorada: limite global por hora atingido");
                        return Mono.<Void>empty();
                    }

                    String token = novoToken();

                    TokenRecuperacaoSenha registro = new TokenRecuperacaoSenha();
                    registro.setTokenHash(hash(token));
                    registro.setEmail(conta.email());
                    registro.setTipoConta(conta.tipo());
                    registro.setCriadoEm(agora);
                    registro.setExpiraEm(agora.plus(VALIDADE));

                    // Só o pedido mais recente vale
                    return tokens.deleteByEmail(conta.email())
                            .then(tokens.save(registro))
                            .then(emailSender.enviar(
                                    conta.email(),
                                    "Redefinição de senha — Acervo Carmen Lydia",
                                    montarHtml(token),
                                    montarTexto(token)));
                });
    }

    /* ==========================
       2. REDEFINIÇÃO
       ========================== */

    public Mono<Void> redefinir(String token, String novaSenha) {
        return tokens.findByTokenHash(hash(token))
                .switchIfEmpty(Mono.error(new IllegalArgumentException(MENSAGEM_LINK_INVALIDO)))
                .flatMap(registro -> {
                    if (registro.getExpiraEm().isBefore(Instant.now())) {
                        return tokens.delete(registro)
                                .then(Mono.<Void>error(new IllegalArgumentException(MENSAGEM_LINK_INVALIDO)));
                    }

                    String hashSenha = passwordEncoder.encode(novaSenha);

                    return atualizarSenha(registro, hashSenha)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException(MENSAGEM_LINK_INVALIDO)))
                            .then(tokens.deleteByEmail(registro.getEmail()));
                });
    }

    private Mono<?> atualizarSenha(TokenRecuperacaoSenha registro, String hashSenha) {
        String email = registro.getEmail();

        return switch (registro.getTipoConta()) {
            case ADMIN -> admins.findByEmail(email).flatMap(a -> {
                a.setSenha(hashSenha);
                return admins.save(a);
            });
            case ATLETA -> atletas.findByEmail(email).flatMap(a -> {
                a.setSenha(hashSenha);
                a.setAtualizadoEm(Instant.now());
                return atletas.save(a);
            });
            case FOTOGRAFA -> fotografas.findByEmail(email).flatMap(f -> {
                f.setSenha(hashSenha);
                f.setAtualizadoEm(Instant.now());
                return fotografas.save(f);
            });
        };
    }

    /* ==========================
       UTIL
       ========================== */

    /** Mesma ordem do login: admin, atleta, fotógrafa. */
    private Mono<Conta> buscarConta(String email) {
        return admins.findByEmail(email).map(a -> new Conta(TipoConta.ADMIN, a.getEmail()))
                .switchIfEmpty(Mono.defer(() -> atletas.findByEmail(email).map(a -> new Conta(TipoConta.ATLETA, a.getEmail()))))
                .switchIfEmpty(Mono.defer(() -> fotografas.findByEmail(email).map(f -> new Conta(TipoConta.FOTOGRAFA, f.getEmail()))));
    }

    private boolean dentroDoLimiteGlobal() {
        long agora = System.currentTimeMillis();
        long inicio = janelaInicio.get();
        if (agora - inicio > Duration.ofHours(1).toMillis() && janelaInicio.compareAndSet(inicio, agora)) {
            enviosNaJanela.set(0);
        }
        return enviosNaJanela.incrementAndGet() <= LIMITE_GLOBAL_POR_HORA;
    }

    private String novoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String link(String token) {
        return frontendUrl + "/redefinir-senha?token=" + token;
    }

    private String montarTexto(String token) {
        return """
                Olá!

                Recebemos um pedido para redefinir a senha da sua conta no Acervo Carmen Lydia.
                Para escolher uma nova senha, acesse o link abaixo (válido por 1 hora):

                %s

                Se você não fez esse pedido, ignore este e-mail: sua senha continua a mesma.
                """.formatted(link(token));
    }

    private String montarHtml(String token) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;color:#2A211A">
                  <h2 style="font-family:Georgia,serif">Redefinição de senha</h2>
                  <p>Recebemos um pedido para redefinir a senha da sua conta no <strong>Acervo Carmen Lydia</strong>.</p>
                  <p><a href="%1$s" style="display:inline-block;background:#C9A45C;color:#2A211A;padding:12px 24px;text-decoration:none;border-radius:2px">Escolher nova senha</a></p>
                  <p style="font-size:13px;color:#5C5142">O link vale por 1 hora e só pode ser usado uma vez.<br>Se o botão não funcionar, copie e cole este endereço no navegador:<br>%1$s</p>
                  <p style="font-size:13px;color:#5C5142">Se você não fez esse pedido, ignore este e-mail: sua senha continua a mesma.</p>
                </div>
                """.formatted(link(token));
    }
}
