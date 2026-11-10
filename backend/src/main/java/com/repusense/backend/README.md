# RepuSense – Backend 🚀

Backend de **RepuSense**, una plataforma de prospección comercial y análisis de reputación online mediante inteligencia artificial.

Desarrollado con **Java y Spring Boot**, permite buscar comercios reales, almacenar información en PostgreSQL, gestionar leads y analizar la reputación de los negocios mediante Gemini AI.

## 🎯 Objetivo

Facilitar la identificación de oportunidades comerciales para agencias de marketing y reputación digital, mediante la búsqueda de negocios y el análisis de sus calificaciones y reseñas disponibles en internet.

## 🛠️ Stack tecnológico

- **Lenguaje:** Java 21.
- **Framework:** Spring Boot 3.2.3.
- **Persistencia:** Spring Data JPA / Hibernate.
- **Base de datos:** PostgreSQL.
- **Autenticación y seguridad:** Spring Security y JWT.
- **Inteligencia artificial:** Google Gemini API.
- **Búsqueda de comercios:** SerpApi / Google Maps.
- **Gestión de dependencias:** Maven.
- **Otras herramientas:** Lombok.

## ⚙️ Funcionalidades principales

- Búsqueda de comercios reales por ciudad y rubro.
- Consulta de información comercial mediante SerpApi y Google Maps.
- Almacenamiento de comercios y leads en PostgreSQL.
- Gestión y actualización del estado de los leads.
- Análisis de reputación mediante inteligencia artificial con Gemini.
- Consulta de análisis guardados.
- Registro e inicio de sesión de usuarios.
- Control de acceso mediante roles USER y ADMIN.
- Administración de comercios, incluyendo eliminación individual y múltiple.
- Historial de búsquedas realizadas.

## 📋 Requisitos previos

Para ejecutar el backend localmente necesitás:

- Java JDK 21.
- PostgreSQL.
- Maven o Maven Wrapper.
- Claves de acceso para los servicios externos utilizados.

## 🗄️ Configuración de la base de datos

Crear la base de datos en PostgreSQL:

```sql
CREATE DATABASE repusense_db;
```

Configurar las credenciales de conexión en `application.properties`, de acuerdo con la configuración del entorno local.

## 🔐 Variables de entorno

El proyecto utiliza variables de entorno para configurar servicios externos, seguridad y administración.

| Variable | Descripción |
|---|---|
| `GEMINI_API_KEY` | Clave de acceso a Google Gemini. |
| `SERPAPI_API_KEY` | Clave de acceso a SerpApi. |
| `JWT_SECRET` | Clave utilizada para firmar tokens JWT. |
| `JWT_EXPIRATION` | Tiempo de expiración de los tokens. |
| `FRONTEND_ALLOWED_ORIGINS` | Orígenes permitidos para las solicitudes del frontend. |
| `APP_ADMIN_BOOTSTRAP_EMAIL` | Correo del administrador inicial. |
| `APP_ADMIN_BOOTSTRAP_PASSWORD` | Contraseña para inicializar el administrador. |

**Importante:** no subir archivos `.env`, contraseñas ni claves privadas al repositorio.

## ▶️ Cómo ejecutar el proyecto

Clonar el repositorio:

```bash
git clone URL_DEL_REPOSITORIO
```

Ingresar a la carpeta del backend:

```bash
cd backend
```

Ejecutar con Maven Wrapper en Windows:

```cmd
mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

El servidor estará disponible en:

```text
http://localhost:8080
```

## 🔌 Endpoints principales

### Autenticación

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registro de usuarios. |
| POST | `/api/auth/login` | Inicio de sesión. |

### Leads

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/leads` | Consulta de leads. |
| POST | `/api/v1/leads/{id}/analyze` | Análisis de un lead mediante IA. |
| PATCH | `/api/v1/leads/{id}/status` | Actualización del estado comercial. |

### Búsquedas

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/searches` | Inicia una búsqueda de comercios. |

### Administración

Endpoints disponibles exclusivamente para usuarios con rol ADMIN:

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/admin/businesses` | Consulta de comercios. |
| DELETE | `/api/admin/businesses/{id}` | Eliminación individual. |
| DELETE | `/api/admin/businesses/bulk` | Eliminación múltiple. |

## 🏗️ Arquitectura

El backend utiliza una arquitectura organizada por responsabilidades:

- **Controllers:** reciben y gestionan las solicitudes HTTP.
- **Services:** implementan la lógica de negocio e integraciones externas.
- **Repositories:** gestionan el acceso y la persistencia de datos mediante JPA.
- **Entities:** representan las entidades almacenadas en PostgreSQL.
- **Security:** administra autenticación, autorización y roles.

## 👥 Proyecto

RepuSense fue desarrollado en el marco de una simulación de trabajo colaborativo de **No Country**, aplicando metodologías ágiles y trabajo en equipo.

**Proyecto:** RepuSense – Radar de Reputación en Crisis.