# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy root POM first so Maven can resolve the multi-module parent.
COPY pom.xml /workspace/pom.xml
COPY services/analysis-service/pom.xml /workspace/services/analysis-service/pom.xml

# Pre-fetch dependencies before copying source (improves layer caching).
RUN mvn -pl services/analysis-service -am dependency:go-offline -q

COPY services/analysis-service/src /workspace/services/analysis-service/src

RUN mvn -pl services/analysis-service -am package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /workspace/services/analysis-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
