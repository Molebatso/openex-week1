# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM gradle:8.12-jdk21 AS builder

WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -q

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Non-root user for security
RUN addgroup -S openex && adduser -S openex -G openex
USER openex

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
