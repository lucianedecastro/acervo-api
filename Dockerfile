# ---------------------------
# 🏗️ Estágio 1: Build da Aplicação
# ---------------------------
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa dependências (cache inteligente)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código e gera o JAR
COPY src ./src
RUN mvn clean package -DskipTests -DcompilerArgs=-parameters

# ---------------------------
# 🚀 Estágio 2: Imagem de Execução (Leve)
# ---------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

LABEL maintainer="Luciane de Castro <email@dominio.com>" \
      project="acervo-carmen-lydia" \
      description="API Java com Spring Boot e Firestore"

# Copia o artefato final
COPY --from=build /app/target/*.jar app.jar

# Expondo porta padrão
EXPOSE 8080
ENV PORT=8080

# Inicializa o app
ENTRYPOINT ["java", "-jar", "app.jar"]
