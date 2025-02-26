FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY api-gateway/mvnw api-gateway/pom.xml ./api-gateway/
COPY api-gateway/.mvn ./api-gateway/.mvn

WORKDIR /app/api-gateway
RUN ./mvnw dependency:go-offline -B || true

COPY api-gateway/src ./src
ARG MAVEN_PROFILE=dev
RUN ./mvnw clean package -P${MAVEN_PROFILE} -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/api-gateway/target/*.jar app.jar

EXPOSE 9098
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
