# Estágio 1: Build da Aplicação com Maven
# Usamos uma imagem que já tem o Maven e o JDK 17 instalados.
FROM maven:3.8-openjdk-17 AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o ficheiro de configuração do Maven para aproveitar o cache de dependências
COPY pom.xml .

# Baixa todas as dependências do projeto
RUN mvn dependency:go-offline

# Copia todo o resto do código-fonte
COPY src ./src

# Executa o build, criando o ficheiro .jar. A flag -DskipTests acelera o processo.
# A flag -DcompilerArgs=-parameters garante que nosso erro anterior seja resolvido.
RUN mvn package -DskipTests -DcompilerArgs=-parameters


# Estágio 2: Imagem Final de Execução
# Usamos uma imagem mínima, apenas com o Java 17, para tornar a nossa imagem final mais leve e segura.
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o ficheiro .jar gerado no estágio anterior para a imagem final
COPY --from=build /app/target/acervo-api-0.0.1-SNAPSHOT.jar .

# Expõe a porta que a aplicação vai usar (será definida pela variável de ambiente PORT do Cloud Run)
EXPOSE 8080

# Comando para iniciar a aplicação quando o container arrancar
ENTRYPOINT ["java", "-jar", "acervo-api-0.0.1-SNAPSHOT.jar"]