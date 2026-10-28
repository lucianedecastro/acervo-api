package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import br.com.acervodaatletabrasileira.acervoapi.model.TokenRecuperacaoSenha;
import br.com.acervodaatletabrasileira.acervoapi.model.TokenRecuperacaoSenha.TipoConta;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.FotografaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.TokenRecuperacaoSenhaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecuperacaoSenhaServiceTest {

    TokenRecuperacaoSenhaRepository tokens = mock(TokenRecuperacaoSenhaRepository.class);
    UsuarioAdminRepository admins = mock(UsuarioAdminRepository.class);
    AtletaRepository atletas = mock(AtletaRepository.class);
    FotografaRepository fotografas = mock(FotografaRepository.class);
    EmailSender email = mock(EmailSender.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    List<TokenRecuperacaoSenha> banco = new ArrayList<>();
    RecuperacaoSenhaService service;

    @BeforeEach
    void setUp() {
        when(admins.findByEmail(any())).thenReturn(Mono.empty());
        when(atletas.findByEmail(any())).thenReturn(Mono.empty());
        when(fotografas.findByEmail(any())).thenReturn(Mono.empty());
        when(admins.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(atletas.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(fotografas.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(tokens.deleteByExpiraEmBefore(any())).thenReturn(Mono.empty());
        when(tokens.deleteByEmail(any())).thenAnswer(i -> {
            banco.removeIf(t -> t.getEmail().equals(i.getArgument(0)));
            return Mono.empty();
        });
        when(tokens.delete(any())).thenAnswer(i -> { banco.remove(i.<TokenRecuperacaoSenha>getArgument(0)); return Mono.empty(); });
        when(tokens.save(any())).thenAnswer(i -> { banco.add(i.getArgument(0)); return Mono.just(i.getArgument(0)); });
        when(tokens.findFirstByEmailOrderByCriadoEmDesc(any())).thenAnswer(i -> banco.stream()
                .filter(t -> t.getEmail().equals(i.getArgument(0)))
                .reduce((a, b) -> b).map(Mono::just).orElse(Mono.empty()));
        when(tokens.findByTokenHash(any())).thenAnswer(i -> banco.stream()
                .filter(t -> t.getTokenHash().equals(i.getArgument(0)))
                .findFirst().map(Mono::just).orElse(Mono.empty()));
        when(email.enviar(anyString(), anyString(), anyString(), anyString())).thenReturn(Mono.empty());

        service = new RecuperacaoSenhaService(tokens, admins, atletas, fotografas, encoder, email,
                "https://site.exemplo.com/");
    }

    private Atleta atletaCadastrada(String emailConta) {
        Atleta a = new Atleta();
        a.setEmail(emailConta);
        a.setSenha(encoder.encode("senha-antiga-1"));
        when(atletas.findByEmail(emailConta)).thenReturn(Mono.just(a));
        return a;
    }

    /** Extrai do texto do e-mail o token puro que a usuária receberia no link. */
    private String tokenEnviado(String paraEsperado) {
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(email).enviar(eq(paraEsperado), anyString(), anyString(), texto.capture());
        Matcher m = Pattern.compile("https://site\\.exemplo\\.com/redefinir-senha\\?token=(\\S+)").matcher(texto.getValue());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void pedido_guardaSoOHashEEnviaLinkParaOEmailDaConta() {
        atletaCadastrada("maria@teste.com");

        service.processarSolicitacao("maria@teste.com").block();

        assertThat(banco).hasSize(1);
        String token = tokenEnviado("maria@teste.com");
        assertThat(banco.get(0).getTokenHash()).isEqualTo(RecuperacaoSenhaService.hash(token)).isNotEqualTo(token);
        assertThat(banco.get(0).getTipoConta()).isEqualTo(TipoConta.ATLETA);
        assertThat(banco.get(0).getExpiraEm()).isAfter(Instant.now().plusSeconds(3500));
    }

    @Test
    void pedido_emailDesconhecidoNaoGeraTokenNemEmailENaoFalha() {
        service.processarSolicitacao("ninguem@teste.com").block();

        assertThat(banco).isEmpty();
        verify(email, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void pedido_aceitaEmailDigitadoComOutraCaixa() {
        atletaCadastrada("maria@teste.com");

        service.processarSolicitacao("  Maria@Teste.COM ").block();

        assertThat(banco).hasSize(1);
        tokenEnviado("maria@teste.com");
    }

    @Test
    void pedido_repetidoEmMenosDe60sEIgnorado() {
        atletaCadastrada("maria@teste.com");

        service.processarSolicitacao("maria@teste.com").block();
        service.processarSolicitacao("maria@teste.com").block();

        verify(email, org.mockito.Mockito.times(1)).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void redefinir_trocaASenhaDaAtletaEInvalidaOLink() {
        Atleta a = atletaCadastrada("maria@teste.com");
        service.processarSolicitacao("maria@teste.com").block();
        String token = tokenEnviado("maria@teste.com");

        service.redefinir(token, "senha-nova-123").block();

        assertThat(encoder.matches("senha-nova-123", a.getSenha())).isTrue();
        assertThat(banco).isEmpty();
        assertThatThrownBy(() -> service.redefinir(token, "outra-senha-456").block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redefinir_funcionaParaAdminEFotografa() {
        UsuarioAdmin adm = new UsuarioAdmin();
        adm.setEmail("adm@teste.com");
        when(admins.findByEmail("adm@teste.com")).thenReturn(Mono.just(adm));
        Fotografa fot = new Fotografa();
        fot.setEmail("fot@teste.com");
        when(fotografas.findByEmail("fot@teste.com")).thenReturn(Mono.just(fot));

        service.processarSolicitacao("adm@teste.com").block();
        service.processarSolicitacao("fot@teste.com").block();
        service.redefinir(tokenEnviado("adm@teste.com"), "senha-nova-123").block();
        service.redefinir(tokenEnviado("fot@teste.com"), "senha-nova-456").block();

        assertThat(encoder.matches("senha-nova-123", adm.getSenha())).isTrue();
        assertThat(encoder.matches("senha-nova-456", fot.getSenha())).isTrue();
    }

    @Test
    void redefinir_tokenExpiradoOuInexistenteERecusado() {
        TokenRecuperacaoSenha vencido = new TokenRecuperacaoSenha();
        vencido.setTokenHash(RecuperacaoSenhaService.hash("velho"));
        vencido.setEmail("maria@teste.com");
        vencido.setTipoConta(TipoConta.ATLETA);
        vencido.setCriadoEm(Instant.now().minusSeconds(7200));
        vencido.setExpiraEm(Instant.now().minusSeconds(3600));
        banco.add(vencido);
        Atleta a = atletaCadastrada("maria@teste.com");
        String senhaAntes = a.getSenha();

        assertThatThrownBy(() -> service.redefinir("velho", "senha-nova-123").block())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("expirado");
        assertThatThrownBy(() -> service.redefinir("nao-existe", "senha-nova-123").block())
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(a.getSenha()).isEqualTo(senhaAntes);
        assertThat(banco).isEmpty();
    }
}
