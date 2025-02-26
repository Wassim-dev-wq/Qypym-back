FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY user-service/mvnw user-service/pom.xml ./user-service/
COPY user-service/.mvn ./user-service/.mvn

WORKDIR /app/user-service
RUN ./mvnw dependency:go-offline -B || true

COPY user-service/src ./src
ARG MAVEN_PROFILE=dev
RUN ./mvnw clean package -P${MAVEN_PROFILE} -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/user-service/target/*.jar app.jar

EXPOSE 9090
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]