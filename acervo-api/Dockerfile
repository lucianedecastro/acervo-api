# Estágio 1: Build da Aplicação com Maven
FROM maven:3.8-openjdk-17 AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o ficheiro de configuração do Maven.
# O Dockerfile está em acervo-api/, então 'pom.xml' é o caminho correto.
COPY pom.xml .

# Baixa todas as dependências do projeto
RUN mvn dependency:go-offline

# Copia todo o resto do código-fonte
COPY src ./src

# Executa o build, criando o ficheiro .jar.
RUN mvn package -DskipTests -DcompilerArgs=-parameters


# Estágio 2: Imagem Final de Execução
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o ficheiro .jar gerado no estágio anterior
COPY --from=build /app/target/acervo-api-0.0.1-SNAPSHOT.jar .

# Expõe a porta que a aplicação vai usar
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "acervo-api-0.0.1-SNAPSHOT.jar"]