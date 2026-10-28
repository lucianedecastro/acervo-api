package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaRegistroDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.FotografaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutocadastroServiceTest {

    AtletaRepository atletas = mock(AtletaRepository.class);
    FotografaRepository fotografas = mock(FotografaRepository.class);
    UsuarioAdminRepository admins = mock(UsuarioAdminRepository.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    AtletaService service;

    @BeforeEach
    void setUp() {
        when(admins.findByEmail(any())).thenReturn(Mono.empty());
        when(atletas.findByEmail(any())).thenReturn(Mono.empty());
        when(fotografas.findByEmail(any())).thenReturn(Mono.empty());
        when(atletas.findBySlug(any())).thenReturn(Mono.empty());
        when(atletas.findByCpf(any())).thenReturn(Mono.empty());
        when(atletas.save(any(Atleta.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        service = new AtletaService(atletas, mock(ItemAcervoRepository.class), encoder, null,
                new ContaEmailService(admins, atletas, fotografas));
    }

    private static AtletaRegistroDTO dto(String email, String cpf) {
        return new AtletaRegistroDTO("  Maria Lenk ", email, "senhaforte1", cpf, null);
    }

    @Test
    void registrar_normalizaDadosEDeixaPendente() {
        Atleta a = service.registrar(dto(" Maria@Teste.COM ", "123.456.789-01")).block();

        assertThat(a.getEmail()).isEqualTo("maria@teste.com");
        assertThat(a.getCpf()).isEqualTo("12345678901");
        assertThat(a.getSlug()).isEqualTo("maria-lenk");
        assertThat(a.getStatusAtleta()).isEqualTo(AtletaService.STATUS_PENDENTE_APROVACAO);
        assertThat(a.getStatusVerificacao()).isEqualTo(Atleta.StatusVerificacao.PENDENTE);
        assertThat(a.getContratoAssinado()).isFalse();
        assertThat(encoder.matches("senhaforte1", a.getSenha())).isTrue();
        assertThat(AtletaService.isPublicavel(a)).isFalse();
    }

    @Test
    void registrar_slugRepetidoGanhaSufixo() {
        when(atletas.findBySlug("maria-lenk")).thenReturn(Mono.just(new Atleta()));
        Atleta a = service.registrar(dto("m@x.com", "12345678901")).block();
        assertThat(a.getSlug()).startsWith("maria-lenk-").hasSize("maria-lenk-".length() + 4);
    }

    @Test
    void registrar_cpfInvalidoELancadoComoArgumentoInvalido() {
        assertThatThrownBy(() -> service.registrar(dto("m@x.com", "123")).block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrar_emailJaUsadoPorOutroTipoDeContaRecebe409() {
        when(admins.findByEmail("m@x.com")).thenReturn(Mono.just(new UsuarioAdmin()));
        assertThatThrownBy(() -> service.registrar(dto("m@x.com", "12345678901")).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");

        when(admins.findByEmail("m@x.com")).thenReturn(Mono.empty());
        when(fotografas.findByEmail("m@x.com")).thenReturn(Mono.just(new Fotografa()));
        assertThatThrownBy(() -> service.registrar(dto("m@x.com", "12345678901")).block())
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void registrar_cpfJaCadastradoRecebe409() {
        when(atletas.findByCpf("12345678901")).thenReturn(Mono.just(new Atleta()));
        assertThatThrownBy(() -> service.registrar(dto("novo@x.com", "123.456.789-01")).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void login_aceitaEmailDigitadoComOutraCaixa() {
        Atleta existente = new Atleta();
        existente.setEmail("maria@teste.com");
        existente.setSenha(encoder.encode("senhaforte1"));
        when(atletas.findByEmail("Maria@Teste.com")).thenReturn(Mono.empty());
        when(atletas.findByEmail("maria@teste.com")).thenReturn(Mono.just(existente));
        when(admins.findByEmail(any())).thenReturn(Mono.empty());
        when(fotografas.findByEmail(any())).thenReturn(Mono.empty());

        UserDetails u = new UserDetailsServiceImpl(admins, atletas, fotografas).findByUsername("Maria@Teste.com").block();

        assertThat(u).isNotNull();
        assertThat(u.getUsername()).isEqualTo("maria@teste.com");
        assertThat(u.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ATLETA");
    }
}
