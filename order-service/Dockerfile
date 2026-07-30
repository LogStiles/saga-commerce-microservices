# Build context is the repository root (see docker-compose.yaml).
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Maven wrapper + all module descriptors (needed to resolve the reactor graph)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY outbox/pom.xml outbox/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml

# Only the sources this service needs (outbox is an upstream dependency)
COPY outbox/src outbox/src
COPY order-service/src order-service/src

# -pl order-service -am : build order-service and its upstream deps (outbox) only
RUN ./mvnw -q -pl order-service -am -DskipTests package

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=builder /workspace/order-service/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
