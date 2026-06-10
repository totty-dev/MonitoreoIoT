FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests
# Lista los archivos generados
RUN ls -la /app/target/
# Muestra el contenido del manifiesto del JAR
RUN unzip -p /app/target/app.jar META-INF/MANIFEST.MF || echo "MANIFEST not found"

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
# Verifica el manifiesto también en la etapa final
RUN echo "=== Manifest in final app.jar ===" && unzip -p app.jar META-INF/MANIFEST.MF || echo "MANIFEST not found"
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]