FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiamos todo el repositorio a la imagen
COPY . .

# Nos movemos a la carpeta backend donde está el pom.xml y compilamos
WORKDIR /app/backend
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el jar compilado desde la etapa de build
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]