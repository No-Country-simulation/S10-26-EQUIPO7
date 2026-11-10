# 🗄️ RepuSense - Módulo de Base de Datos

Este módulo contiene la documentación de configuración, conexión y administración de la base de datos PostgreSQL utilizada por **RepuSense – Radar de Reputación en Crisis**.

## 📊 Arquitectura de la base de datos

RepuSense utiliza PostgreSQL para almacenar la información generada y administrada por el sistema.

Entre los principales datos gestionados se encuentran:

- **Business:** información de los comercios encontrados mediante las búsquedas.
- **Lead:** prospectos comerciales, estados de seguimiento y resultados del análisis de reputación.
- **SearchRun:** historial y métricas de las búsquedas realizadas.
- **Reviews:** reseñas utilizadas para evaluar la reputación de los comercios.
- **Usuarios y roles:** información relacionada con la autenticación y los permisos de acceso.

La estructura definitiva depende de las entidades implementadas en el backend.

## 🛠️ Configuración y conexión

### Requisitos previos

- PostgreSQL 15 o superior.
- Base de datos: `repusense_db`.
- Backend desarrollado con Spring Boot y Spring Data JPA.
- Java 21.

### Creación de la base de datos

Desde Windows CMD, ingresar a PostgreSQL:

```bash
"C:\Program Files\PostgreSQL\15\bin\psql.exe" -U postgres
```

Crear la base de datos:

```sql
CREATE DATABASE repusense_db;
```

Conectarse a la base:

```sql
\c repusense_db
```

### Inicialización del esquema

El backend utiliza Spring Data JPA e Hibernate para gestionar las entidades y sincronizar la estructura de las tablas con PostgreSQL.

La configuración utilizada es:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Por lo tanto, las tablas se actualizan al iniciar el backend, según las entidades existentes en el proyecto.

No es necesario ejecutar un archivo `schema.sql` si no está incluido en el repositorio.

## 🔍 Comandos útiles en PostgreSQL

Conectarse a la base de datos:

```sql
\c repusense_db
```

Listar las tablas:

```sql
\dt
```

Consultar la estructura de una tabla:

```sql
\d business
```

Consultar negocios:

```sql
SELECT * FROM business;
```

Consultar leads:

```sql
SELECT * FROM lead;
```

Consultar historial de búsquedas:

```sql
SELECT * FROM search_run;
```

Consultar cantidad de negocios:

```sql
SELECT COUNT(*) FROM business;
```

Consultar cantidad de leads:

```sql
SELECT COUNT(*) FROM lead;
```

Consultar estados de los leads:

```sql
SELECT status, COUNT(*)
FROM lead
GROUP BY status;
```

Salir de PostgreSQL:

```sql
\q
```

## 🔐 Configuración de conexión

La conexión se configura desde el backend mediante `application.properties` y las variables de entorno correspondientes.

Datos de referencia:

| Parámetro | Valor |
|---|---|
| Motor | PostgreSQL |
| Base de datos | repusense_db |
| Puerto | 5432 |
| Usuario | postgres (configurable) |

Las credenciales reales deben configurarse localmente. No deben publicarse contraseñas ni claves privadas en GitHub.

## 🔗 Integración con el backend

El backend Spring Boot utiliza PostgreSQL para:

- Persistir los comercios encontrados.
- Registrar y administrar leads.
- Guardar el historial de búsquedas.
- Almacenar información de reseñas y análisis.
- Gestionar usuarios y permisos de acceso.

## 👥 Proyecto

**RepuSense – Radar de Reputación en Crisis**

Proyecto desarrollado en el marco de la simulación laboral de No Country, equipo S10-26-EQUIPO7.