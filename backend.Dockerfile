FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src

# Instalar Maven y compilar
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# Verificar que config.properties está dentro del JAR
RUN unzip -l /build/target/app.jar | grep config.properties

# Stage 2 - Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiar SOLO el JAR (que ya contiene config.properties)
COPY --from=builder /build/target/app.jar ./app.jar

EXPOSE 8082

# Ejecutar el JAR
CMD ["java", "-jar", "app.jar"]
