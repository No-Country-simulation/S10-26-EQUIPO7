# S10-26-EQUIPO7

## RepuSense – Radar de Reputación en Crisis

Simulación No Country – Plataforma de prospección comercial que identifica negocios con oportunidades de mejora en su reputación online mediante datos de Google Maps e inteligencia artificial.

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Maven
- PostgreSQL
- React y Vite
- SerpApi (búsqueda de comercios)
- Google Gemini (análisis mediante IA)

## Requisitos previos

Antes de ejecutar el proyecto, es necesario tener instalado:

- Java JDK 21
- Maven
- Node.js y npm
- PostgreSQL
- Git

## Configuración de claves API

RepuSense utiliza dos servicios externos que requieren claves API:

| Variable | Servicio | Funcionalidad |
|---|---|---|
| `SERPAPI_API_KEY` | SerpApi | Búsqueda de comercios reales mediante Google Maps. |
| `GEMINI_API_KEY` | Google Gemini | Análisis de reputación mediante inteligencia artificial. |

### 1. Archivo de configuración

Las claves se encuentran en el archivo:

`ai/.env`

El archivo debe contener:

```env
SERPAPI_API_KEY=tu_clave_de_serpapi
GEMINI_API_KEY=tu_clave_de_gemini
```

Cada integrante debe configurar sus propias claves válidas.

**Importante:** no subir el archivo `.env` a GitHub ni compartir las claves públicamente.

### 2. Cargar las claves en Windows CMD

Spring Boot no carga automáticamente las variables del archivo `ai/.env`.

Antes de iniciar el backend, abrir CMD en la carpeta raíz del proyecto:

```cmd
cd /d E:\S10-26-EQUIPO7
```

Cargar la clave de SerpApi:

```cmd
for /f "tokens=1,* delims==" %A in ('findstr /b "SERPAPI_API_KEY=" ai\.env') do set "SERPAPI_API_KEY=%B"
```

Cargar la clave de Gemini:

```cmd
for /f "tokens=1,* delims==" %A in ('findstr /b "GEMINI_API_KEY=" ai\.env') do set "GEMINI_API_KEY=%B"
```

### 3. Verificar las claves

Ejecutar:

```cmd
if defined SERPAPI_API_KEY (echo SerpApi cargada) else (echo SerpApi no cargada)

if defined GEMINI_API_KEY (echo Gemini cargada) else (echo Gemini no cargada)
```

No es necesario mostrar el contenido de las claves.

### 4. Iniciar el backend

En la misma ventana CMD, ejecutar:

```cmd
cd /d E:\S10-26-EQUIPO7\backend
for /f %A in ('powershell -NoProfile -Command "[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))"') do set "JWT_SECRET=%A"
set "JWT_EXPIRATION=3600000"
set "FRONTEND_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173"
mvn spring-boot:run
```

El primer comando genera un secreto JWT aleatorio de 32 bytes. Conservá el
mismo secreto entre reinicios del backend para que las sesiones sigan siendo
válidas; no lo guardes en Git. `JWT_EXPIRATION` se expresa en milisegundos
(3600000 equivale a 60 minutos). `backend/.env.example` documenta estas
variables, pero Spring Boot no carga automáticamente archivos `.env`.
El token se almacena en `localStorage` para conservar la sesión entre recargas;
como cualquier dato accesible desde JavaScript, puede quedar expuesto ante una
vulnerabilidad XSS.

El primer usuario debe registrarse desde la pantalla de acceso. Luego puede
iniciar sesión para entrar al Dashboard. Los endpoints de la aplicación
requieren `Authorization: Bearer <token>`.

Backend disponible en:

`http://localhost:8080`

**Importante:** si se cierra la ventana CMD, las variables cargadas mediante `set` se pierden. Hay que volver a ejecutar los comandos de carga antes de iniciar el backend.

## Iniciar el frontend

Abrir otra ventana CMD:

```cmd
cd /d E:\S10-26-EQUIPO7\FRONT
npm install
npm run dev
```

El frontend estará disponible normalmente en:

`http://localhost:5173`

## Base de datos

El backend utiliza PostgreSQL.

Configuración local actual:

- Base de datos: `repusense_db`
- Usuario: `postgres`
- Puerto: `5432`

Verificar que PostgreSQL esté iniciado y que la base de datos exista antes de ejecutar el backend.

## Funcionalidades principales

- Búsqueda de comercios por ciudad y rubro.
- Obtención de información comercial mediante SerpApi.
- Almacenamiento de negocios y leads en PostgreSQL.
- Consulta y filtrado de clientes potenciales.
- Análisis de reputación mediante Google Gemini.
- Identificación de problemas y oportunidades de mejora.
- Exportación de leads a CSV.

## Solución de problemas

- Si no funciona la búsqueda de comercios, comprobar que `SERPAPI_API_KEY` esté cargada.
- Si falla el análisis con IA, comprobar que `GEMINI_API_KEY` esté cargada y que la configuración de Gemini sea válida.
- Si el backend no conecta, verificar que PostgreSQL esté iniciado y que las credenciales sean correctas.
- Si el frontend no se conecta al backend, comprobar que Spring Boot esté ejecutándose en el puerto 8080.

## Equipo

Proyecto desarrollado en el marco de la simulación de No Country – S10-26-EQUIPO7.