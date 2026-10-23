FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
WORKDIR /app/backend
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080

# Forzamos la URL correcta directo en la ejecución de Java
ENTRYPOINT ["java", \
  "-Dspring.datasource.url=jdbc:postgresql://dpg-dbdj4tmi0phs73a02m4g-a.oregon-postgres.render.com:5432/repusense_db_ugin", \
  "-Dspring.datasource.username=repusense_db_ugin_user", \
  "-Dspring.datasource.password=ThrDNy3q9LHGRKuNdB6wuIeEoj297tvn", \
  "-Dspring.datasource.driver-class-name=org.postgresql.Driver", \
  "-Dspring.jpa.hibernate.ddl-auto=update", \
  "-Dspring.jpa.show-sql=true", \
  "-Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect", \
  "-jar", "app.jar"]