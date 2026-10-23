FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /app

# Copiamos todo el repositorio a la imagen
COPY . .

# Nos movemos a la carpeta backend donde está el pom.xml y compilamos
WORKDIR /app/backend
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copiamos el jar compilado desde la etapa de build (ajustá el nombre del jar si es necesario)
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]