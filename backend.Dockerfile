# Etapa 1: compilación
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests
# Verifica que el JAR se generó
RUN ls -la /app/target/

# Etapa 2: ejecución
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copia explícitamente el JAR con nombre conocido
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]