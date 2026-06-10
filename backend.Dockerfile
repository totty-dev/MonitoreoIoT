
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn package -DskipTests \

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/target/MonitoreoIoT-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/config.properties config.properties
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]