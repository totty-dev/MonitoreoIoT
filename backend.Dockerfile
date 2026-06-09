FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/MonitoreoIoT-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources/config.properties config.properties
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]