# RepuSense – Radar de Reputación en Crisis 🚀

**S10-26-EQUIPO7 | Simulación No Country**

RepuSense es una plataforma de prospección comercial que permite identificar negocios con oportunidades de mejora en su reputación online mediante información de Google Maps e inteligencia artificial.

El sistema permite buscar comercios reales por ciudad y rubro, consultar sus calificaciones y reseñas disponibles, analizar su reputación mediante IA y gestionar oportunidades comerciales desde un único panel de control.

## 🎯 Objetivo del proyecto

Facilitar el trabajo de agencias de marketing y reputación digital mediante la identificación de comercios que podrían necesitar mejorar su presencia y reputación online.

RepuSense automatiza parte del proceso de búsqueda, recopilación de información y análisis de negocios, transformando esos datos en oportunidades de prospección comercial.

## ✨ Funcionalidades principales

- **Búsqueda de comercios reales:** búsqueda por ciudad y rubro mediante SerpApi y Google Maps.
- **Alcance internacional:** consultas de comercios en distintas ciudades y países.
- **Gestión de leads:** almacenamiento, consulta, filtrado y seguimiento de comercios.
- **Análisis con inteligencia artificial:** evaluación de reputación comercial mediante Google Gemini.
- **Identificación de oportunidades:** detección de problemas y aspectos que pueden mejorarse según los datos disponibles.
- **Seguimiento comercial:** actualización del estado de los leads (NEW, CONTACTED, ANALYZED).
- **Historial de búsquedas:** consulta de las búsquedas realizadas.
- **Exportación CSV:** descarga de los resultados para su análisis externo.
- **Administración de comercios:** consulta, selección y eliminación individual o múltiple de registros.
- **Autenticación y autorización:** registro, inicio de sesión y control de acceso mediante JWT y roles USER / ADMIN.

## 🛠️ Tecnologías utilizadas

### Backend
- Java 21.
- Spring Boot 3.2.3.
- Spring Data JPA.
- Hibernate.
- Spring Security.
- JWT.
- Maven.
- Lombok.

### Frontend
- React.
- Vite.
- JavaScript.
- HTML5.
- CSS3.

### Base de datos
- PostgreSQL.

### Servicios externos
- **SerpApi:** búsqueda de comercios e información de Google Maps.
- **Google Gemini API:** análisis de reputación mediante inteligencia artificial.

## 🏗️ Arquitectura del sistema

El proyecto está organizado en tres componentes principales:

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| Frontend | React + Vite | Interfaz de usuario, panel de prospección y administración. |
| Backend | Java + Spring Boot | API REST, lógica de negocio, seguridad e integraciones externas. |
| Base de datos | PostgreSQL | Persistencia de usuarios, comercios, leads, reseñas e historial. |

### Flujo general de funcionamiento

1. El usuario ingresa a la plataforma mediante autenticación.
2. Selecciona una ciudad y un rubro para realizar una búsqueda.
3. El backend consulta información mediante SerpApi y Google Maps.
4. Los resultados válidos se almacenan en PostgreSQL.
5. El usuario puede consultar y gestionar los comercios encontrados.
6. Gemini IA analiza la información disponible y genera resultados de reputación.
7. Los análisis quedan almacenados para su posterior consulta.

## 📂 Estructura del proyecto

```text
S10-26-EQUIPO7/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── README.md
│
├── FRONT/
│   ├── src/
│   ├── package.json
│   └── README.md
│
├── ai/
│   ├── .env
│   └── ...
│
├── .gitignore
└── README.md
```

La estructura representa los principales módulos del proyecto. La documentación específica de cada componente se encuentra en su respectivo README.

## 📋 Requisitos previos

Antes de ejecutar el proyecto, es necesario tener instalado:

- Java JDK 21.
- Maven o Maven Wrapper.
- Node.js y npm.
- PostgreSQL.
- Git.

También se necesitan claves válidas para los servicios externos utilizados.

## 🔑 Configuración de claves API

RepuSense utiliza dos servicios externos que requieren claves API:

| Variable | Servicio | Funcionalidad |
|---|---|---|
| `SERPAPI_API_KEY` | SerpApi | Búsqueda de comercios mediante Google Maps. |
| `GEMINI_API_KEY` | Google Gemini | Análisis de reputación mediante IA. |

### 1. Archivo de configuración

En el directorio `ai/` se utiliza un archivo `.env` con el siguiente contenido:

```env
SERPAPI_API_KEY=tu_clave_de_serpapi
GEMINI_API_KEY=tu_clave_de_gemini
```

Cada integrante debe configurar sus propias claves válidas.

**Importante:** nunca subir el archivo `.env` ni publicar claves privadas en GitHub.

### 2. Cargar las claves en Windows CMD

Spring Boot no carga automáticamente las variables del archivo `ai/.env`.

Abrir CMD en la carpeta raíz del proyecto:

```cmd
cd /d E:\S10-26-EQUIPO7
```

Cargar SerpApi:

```cmd
for /f "tokens=1,* delims==" %A in ('findstr /b "SERPAPI_API_KEY=" ai\.env') do set "SERPAPI_API_KEY=%B"
```

Cargar Gemini:

```cmd
for /f "tokens=1,* delims==" %A in ('findstr /b "GEMINI_API_KEY=" ai\.env') do set "GEMINI_API_KEY=%B"
```

### 3. Verificar las claves

```cmd
if defined SERPAPI_API_KEY (echo SerpApi cargada) else (echo SerpApi no cargada)

if defined GEMINI_API_KEY (echo Gemini cargada) else (echo Gemini no cargada)
```

No es necesario mostrar el contenido de las claves.

## 🗄️ Configuración de la base de datos

El backend utiliza PostgreSQL.

Configuración local actual:

- **Base de datos:** `repusense_db`
- **Usuario:** `postgres`
- **Puerto:** `5432`

Crear la base de datos:

```sql
CREATE DATABASE repusense_db;
```

Verificar que PostgreSQL esté iniciado y que las credenciales de conexión estén correctamente configuradas en `backend/src/main/resources/application.properties`.

## ▶️ Ejecución del proyecto

### 1. Iniciar el backend

Abrir una ventana CMD y cargar las claves API como se explicó anteriormente.

Ingresar a la carpeta del backend:

```cmd
cd /d E:\S10-26-EQUIPO7\backend
```

Configurar las variables de seguridad:

```cmd
set "JWT_SECRET=TU_SECRETO_JWT_SEGURO_Y_ESTABLE"
set "JWT_EXPIRATION=3600000"
set "FRONTEND_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173"
```

**Importante sobre JWT:** utilizar un secreto seguro y conservar el mismo valor entre reinicios del backend. No publicar este secreto ni subirlo al repositorio. `JWT_EXPIRATION` se expresa en milisegundos; `3600000` equivale a 60 minutos.

Ejecutar Spring Boot:

```cmd
mvnw.cmd spring-boot:run
```

El backend estará disponible en:

`http://localhost:8080`

### 2. Iniciar el frontend

Abrir una segunda ventana CMD:

```cmd
cd /d E:\S10-26-EQUIPO7\FRONT
```

Instalar las dependencias:

```cmd
npm install
```

Iniciar el servidor de desarrollo:

```cmd
npm run dev
```

El frontend estará disponible normalmente en:

`http://localhost:5173`

### 3. Acceso a la aplicación

El usuario puede registrarse desde la pantalla de acceso e iniciar sesión para ingresar al panel de prospección.

Las rutas protegidas requieren autenticación mediante:

`Authorization: Bearer <token>`

El token se almacena en `localStorage` para conservar la sesión entre recargas. Como cualquier dato accesible desde JavaScript, debe protegerse frente a posibles vulnerabilidades XSS.

**Importante:** las variables cargadas mediante `set` en CMD se pierden al cerrar esa ventana. Deben configurarse nuevamente al iniciar el backend.

## 🔐 Seguridad y roles

La aplicación implementa autenticación mediante JWT y control de acceso por roles:

- **USER:** acceso a las funcionalidades generales de la plataforma.
- **ADMIN:** acceso adicional al módulo de administración de comercios.

El registro público asigna el rol USER. La inicialización del administrador se configura mediante variables de entorno.

## 📚 Documentación por módulos

Para conocer los detalles técnicos de cada componente:

- **Backend:** `backend/README.md` – configuración, API REST, seguridad e integraciones.
- **Frontend:** `FRONT/README.md` – instalación, componentes e interfaz de usuario.
- **Base de datos:** documentación del modelo y configuración de PostgreSQL.

## 🛠️ Solución de problemas

| Problema | Posible solución |
|---|---|
| No funciona la búsqueda | Verificar que `SERPAPI_API_KEY` esté cargada y sea válida. |
| Falla el análisis con IA | Comprobar `GEMINI_API_KEY` y la disponibilidad del servicio Gemini. |
| El backend no conecta | Verificar PostgreSQL, la base de datos y las credenciales. |
| El frontend no conecta con el backend | Comprobar que Spring Boot esté ejecutándose en el puerto 8080. |
| Error de autenticación | Verificar la configuración JWT y que el frontend envíe el token. |
| Error de CORS | Revisar `FRONTEND_ALLOWED_ORIGINS`. |

## 👥 Equipo

Proyecto desarrollado en el marco de la simulación de trabajo colaborativo de **No Country – S10-26-EQUIPO7**.

Se aplicaron prácticas de desarrollo colaborativo, integración de tecnologías, organización por módulos y trabajo en equipo.

---

**RepuSense – Radar de Reputación en Crisis**

*Tecnología e inteligencia artificial aplicadas a la prospección comercial.*