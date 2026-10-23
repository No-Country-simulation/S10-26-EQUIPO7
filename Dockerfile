FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
WORKDIR /app/backend
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080

# Forzamos la lectura de las variables de entorno de Render al iniciar Java
ENTRYPOINT ["java", \
  "-Dspring.datasource.url=${SPRING_DATASOURCE_URL}", \
  "-Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME}", \
  "-Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD}", \
  "-jar", "app.jar"]