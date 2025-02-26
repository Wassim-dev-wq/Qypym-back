FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY match-service/mvnw match-service/pom.xml ./match-service/
COPY match-service/.mvn ./match-service/.mvn

WORKDIR /app/match-service
RUN ./mvnw dependency:go-offline -B || true

COPY match-service/src ./src
ARG MAVEN_PROFILE=dev
RUN ./mvnw clean package -P${MAVEN_PROFILE} -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/match-service/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]