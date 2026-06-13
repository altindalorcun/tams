# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy root POM and all sibling service POMs so Maven can resolve the multi-module parent
COPY pom.xml /workspace/pom.xml
COPY services/api-gateway/pom.xml /workspace/services/api-gateway/pom.xml
COPY services/auth-service/pom.xml /workspace/services/auth-service/pom.xml
COPY services/rule-service/pom.xml /workspace/services/rule-service/pom.xml
COPY services/analysis-service/pom.xml /workspace/services/analysis-service/pom.xml

# Prefetch dependencies before copying source (layer cache optimisation)
RUN mvn -pl services/auth-service -am dependency:go-offline -q

COPY services/auth-service/src /workspace/services/auth-service/src

RUN mvn -pl services/auth-service -am package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /workspace/services/auth-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
