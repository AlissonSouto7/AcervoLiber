# syntax=docker/dockerfile:1.7
# =============================================================================
# Stage 1 — build com Maven
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copia primeiro o pom.xml para cachear o download de dependencias.
# Mudancas em src/ nao invalidam essa camada.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Agora copia o codigo e empacota
COPY src ./src
RUN mvn clean package -DskipTests -B \
    && mv target/liber-*.jar target/app.jar

# =============================================================================
# Stage 2 — runtime minimo (JRE Alpine)
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

# curl para o HEALTHCHECK
RUN apk add --no-cache curl

# Usuario nao-root
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder --chown=app:app /build/target/app.jar app.jar

USER app
EXPOSE 8080

# Configuracoes recomendadas para containers (limites do cgroup)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Healthcheck do proprio container (Docker monitora)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
