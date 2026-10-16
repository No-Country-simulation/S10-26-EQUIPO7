# RepuSense Backend MVP 🚀

Backend desarrollado con **Spring Boot** y **PostgreSQL** para la gestión de leads y análisis de reputación comercial de negocios.

## 🛠️ Stack Tecnológico
- **Java:** 21
- **Spring Boot:** 3.2.3
- **Spring Data JPA / Hibernate**
- **Base de Datos:** PostgreSQL
- **Herramientas:** Maven, Lombok

---

## ⚙️ Requisitos Previos
Asegúrate de tener instalado en tu entorno local:
- Java JDK 21
- PostgreSQL (configurado y ejecutándose)
- Maven (o utilizar el wrapper incluido `mvnw`)

---

## 📂 Configuración de la Base de Datos

1. Abre tu cliente de PostgreSQL (o consola `psql`) y crea la base de datos:
   ```sql
   CREATE DATABASE repusense_db;

   Cómo Ejecutar el Proyecto
Clona el repositorio y sitúate en la carpeta del backend:

Bash
cd backend
Ejecuta la aplicación utilizando Maven Wrapper:

En Windows (CMD / PowerShell):

DOS
mvnw spring-boot:run
En Linux / Mac:

Bash
./mvnw spring-boot:run
El servidor iniciará automáticamente en el puerto 8080.

🔌 Endpoints Principales (API REST)
Leads
GET /api/v1/leads: Obtiene la lista paginada de leads (soporta filtros opcionales por city y status).

PATCH /api/v1/leads/{id}/status: Actualiza el estado comercial de un lead específico (ej. a CONTACTED).