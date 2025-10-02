# Étape 1 : Build avec Maven + JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier pom.xml et src
COPY pom.xml .
COPY src ./src

# Construire le projet sans tests
RUN mvn clean package -DskipTests

# Étape 2 : Image minimale avec JDK 21 uniquement
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copier le jar généré
COPY --from=builder /app/target/fineract-hook-kafka-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port (selon application.yml → port: 9999)
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
