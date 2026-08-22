# Etapa 1: Build con Maven y JDK 21
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Runtime ligero con JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios para seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=local
ENV PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
