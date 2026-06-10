# ----- ETAPA 1: Compilación -----
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copiamos el archivo de configuración de Maven y las dependencias primero
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el código fuente de tu backend
COPY src ./src

# Compilamos asegurando que se ejecute el "repackage" de Spring Boot
RUN mvn clean package -DskipTests

# ----- ETAPA 2: Ejecución -----
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copiamos EXCLUSIVAMENTE el jar ejecutable generado en la etapa anterior
# Nota: Si tu pom.xml genera un nombre específico de jar, cambialo acá (ej: MonitoreoIoT-0.0.1-SNAPSHOT.jar)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]