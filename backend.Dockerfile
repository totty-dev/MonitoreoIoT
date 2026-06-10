FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src

# Instalar Maven y compilar
RUN apt-get update && apt-get install -y maven

# Compilar con verbose
RUN mvn clean package -DskipTests -X

# Diagnóstico - Ver qué JARs se generaron
RUN echo "=== JAR Files Generated ===" && \
    ls -lah /build/target/ && \
    echo "" && \
    echo "=== Checking app.jar manifest ===" && \
    (unzip -p /build/target/app.jar META-INF/MANIFEST.MF || echo "ERROR: No manifest found!") && \
    echo "" && \
    echo "=== All files in target ===" && \
    find /build/target -type f -name "*.jar"

# Stage 2 - Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiar el JAR
COPY --from=builder /build/target/app.jar ./app.jar

# Copiar config desde builder
COPY --from=builder /build/src/main/resources/config.properties ./config.properties

# Verificar que el JAR llegó correctamente
RUN echo "=== Verifying JAR in runtime image ===" && \
    ls -lah /app/app.jar && \
    unzip -p /app/app.jar META-INF/MANIFEST.MF

EXPOSE 8082

# CMD con debugging
CMD java -jar /app/app.jar
