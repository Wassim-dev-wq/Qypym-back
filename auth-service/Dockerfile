FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY auth-service/mvnw auth-service/pom.xml ./auth-service/
COPY auth-service/.mvn ./auth-service/.mvn

WORKDIR /app/auth-service
RUN ./mvnw dependency:go-offline -B || true

COPY auth-service/src ./src
ARG MAVEN_PROFILE=dev
RUN ./mvnw clean package -P${MAVEN_PROFILE} -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/auth-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]