# Build context is the repository root (see docker-compose.yaml).
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY outbox/pom.xml outbox/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY e2e-tests/pom.xml e2e-tests/pom.xml

COPY outbox/src outbox/src
COPY payment-service/src payment-service/src

RUN ./mvnw -q -pl payment-service -am -DskipTests package

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=builder /workspace/payment-service/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
