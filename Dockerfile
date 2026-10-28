# ---------------------------
# Estágio 1: build
# ---------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Dependências primeiro, para aproveitar o cache de camadas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Os testes rodam no CI (GitHub Actions); aqui só empacota
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------------
# Estágio 2: execução
# ---------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

LABEL project="acervo-carmen-lydia" \
      description="API Java (Spring Boot WebFlux) com MongoDB"

# Não roda como root
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar
USER spring

EXPOSE 8080
ENV PORT=8080

# Memória limitada pelo container; falha rápido em OOM para a hospedagem reiniciar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

# O ENTRYPOINT usa exec para o java receber SIGTERM (shutdown gracioso)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
