# ---------------------------
# 🏗️ Estágio 1: Build da Aplicação
# ---------------------------
FROM maven:3.8.8-eclipse-temurin-17 AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o arquivo pom.xml e baixa as dependências primeiro (cache inteligente)
COPY pom.xml .

# Baixa dependências antes de copiar o código (melhora cache entre builds)
RUN mvn dependency:go-offline -B

# Copia o código-fonte para dentro do container
COPY src ./src

# Compila o projeto e gera o JAR
RUN mvn clean package -DskipTests -DcompilerArgs=-parameters


# ---------------------------
# 🚀 Estágio 2: Imagem de Execução (Leve)
# ---------------------------
FROM eclipse-temurin:17-jre-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia o JAR gerado do estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta da aplicação (Cloud Run usa variável PORT, mas 8080 é padrão local)
EXPOSE 8080

# Usa variável de ambiente do Cloud Run (fallback 8080)
ENV PORT=8080

# Inicia a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
