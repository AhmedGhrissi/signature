# Dockerfile pour l'API E-Signature
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copier les fichiers Maven
COPY pom.xml .
COPY src ./src

# Installer Maven et compiler l'application
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    mv target/*.jar app.jar

# Image finale
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Créer les répertoires nécessaires
RUN mkdir -p /app/uploads /app/signed-documents /app/certificates

# Copier le JAR depuis le builder
COPY --from=builder /app/app.jar .

# Variables d'environnement
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8080

# Exposer le port
EXPOSE 8080

# Santé check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

# Démarrer l'application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
