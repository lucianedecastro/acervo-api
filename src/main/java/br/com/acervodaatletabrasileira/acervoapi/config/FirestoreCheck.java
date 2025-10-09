package br.com.acervodaatletabrasileira.acervoapi.config;

import com.google.cloud.firestore.Firestore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;

import java.lang.reflect.Field;
import java.util.Arrays;

@Configuration
@EnableReactiveFirestoreRepositories(basePackages = "br.com.acervodaatletabrasileira.acervoapi.repository")
public class FirestoreCheck {

    private static final Logger log = LoggerFactory.getLogger(FirestoreCheck.class);

    private final Firestore firestore;

    public FirestoreCheck(Firestore firestore) {
        this.firestore = firestore;
    }

    @PostConstruct
    public void verifyConnectionAndModel() {
        verifyFirestoreConnection();
        verifyFirestoreModel(br.com.acervodaatletabrasileira.acervoapi.model.Atleta.class);
    }

    /**
     * Teste simples de conexão com o Firestore
     */
    private void verifyFirestoreConnection() {
        try {
            log.info("🔍 Verificando conexão com o Google Firestore...");

            var collections = firestore.listCollections();
            int count = 0;
            for (var col : collections) {
                count++;
                log.debug("📁 Coleção detectada: {}", col.getId());
            }

            if (count == 0) {
                log.info("✅ Conectado ao Firestore, mas nenhuma coleção foi encontrada ainda.");
            } else {
                log.info("✅ Conexão com o Firestore verificada com sucesso ({} coleções).", count);
            }

        } catch (Exception e) {
            log.error("❌ Erro ao verificar a conexão com o Firestore!", e);
        }
    }

    /**
     * Teste extra: verifica se o modelo possui um campo anotado com @Id
     */
    private void verifyFirestoreModel(Class<?> clazz) {
        log.info("🔎 Verificando anotações do modelo: {}", clazz.getSimpleName());

        Field[] fields = clazz.getDeclaredFields();
        boolean idFound = Arrays.stream(fields)
                .anyMatch(f -> f.isAnnotationPresent(Id.class));

        if (idFound) {
            Field idField = Arrays.stream(fields)
                    .filter(f -> f.isAnnotationPresent(Id.class))
                    .findFirst()
                    .orElse(null);

            log.info("✅ Campo @Id detectado no modelo '{}' → nome do campo: '{}', tipo: {}",
                    clazz.getSimpleName(),
                    idField != null ? idField.getName() : "(desconhecido)",
                    idField != null ? idField.getType().getSimpleName() : "(?)"
            );
        } else {
            log.error("❌ Nenhum campo com @Id foi encontrado no modelo '{}'.", clazz.getSimpleName());
            log.error("🧩 Verifique se a classe '{}' contém um campo como:", clazz.getSimpleName());
            log.error("    @Id\n    private String id;\n");
            log.error("E se a dependência spring-cloud-gcp-data-firestore está corretamente configurada.");
        }
    }
}

