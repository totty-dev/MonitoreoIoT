FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiar el JAR ya compilado de tu máquina
COPY target/app.jar ./app.jar

EXPOSE 8082

CMD ["java", "-jar", "app.jar"]
