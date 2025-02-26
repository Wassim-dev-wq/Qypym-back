FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY service-registry/mvnw service-registry/pom.xml ./service-registry/
COPY service-registry/.mvn ./service-registry/.mvn

WORKDIR /app/service-registry
RUN ./mvnw dependency:go-offline -B || true

COPY service-registry/src ./src
ARG MAVEN_PROFILE=dev
RUN ./mvnw clean package -P${MAVEN_PROFILE} -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/service-registry/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]