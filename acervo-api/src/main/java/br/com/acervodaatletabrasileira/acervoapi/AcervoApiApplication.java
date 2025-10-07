package br.com.acervodaatletabrasileira.acervoapi;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
// NOVO: Define as informações gerais da API
@OpenAPIDefinition(info = @Info(title = "Acervo API", version = "1.0", description = "API para o Acervo Carmen Lydia da Mulher Brasileira no Esporte"))
// NOVO: Define o esquema de segurança JWT para a documentação
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer"
)
public class AcervoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcervoApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner initAdminUser(UsuarioAdminRepository repository, PasswordEncoder passwordEncoder) {
		return args -> {
			String adminEmail = "admin@acervo.com";
			repository.findByEmail(adminEmail).hasElement().subscribe(exists -> {
				if (!exists) {
					UsuarioAdmin admin = UsuarioAdmin.builder()
							.nome("Admin Padrão")
							.email(adminEmail)
							.senha(passwordEncoder.encode("admin123"))
							.build();
					repository.save(admin).subscribe(u -> System.out.println("Usuário admin padrão criado: " + u.getEmail()));
				}
			});
		};
	}
}