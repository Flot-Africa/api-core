# Étape de construction
FROM ghcr.io/graalvm/graalvm-ce:latest as builder

# Installation des outils nécessaires à la compilation
RUN gu install native-image
RUN microdnf install -y maven

# Définition du répertoire de travail
WORKDIR /build

# Copie du fichier POM et des sources
COPY pom.xml .
COPY src src/

# Construction de l'image native
RUN mvn clean package -Pnative -DskipTests

# Étape d'exécution
FROM ubuntu:22.04

WORKDIR /app

# Copie de l'exécutable natif depuis l'étape de construction
COPY --from=builder /build/target/*-runner /app/application

# Attribution des droits d'exécution
RUN chmod +x /app/application

# Exposition du port de l'application (à ajuster selon vos besoins)
EXPOSE 8080

# Lancement de l'application native
CMD ["./application"]