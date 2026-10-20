# 🗄️ RepuSense - Módulo de Base de Datos

Este módulo contiene la definición del esquema relacional y las instrucciones de configuración para la base de datos PostgreSQL de **RepuSense**.

---

## 📊 Arquitectura del Esquema

El sistema consta de **3 tablas principales**:

1. **`business`**: Guarda los datos comerciales e institucionales de los locales rastreados.
   - Garantiza la unicidad mediante la clave única `(source, source_business_id)`.
2. **`lead`**: Almacena los prospectos del pipeline de ventas, vinculados a cada negocio, junto con los diagnósticos y sugerencias generados por la Inteligencia Artificial (Google Gemini).
3. **`search_run`**: Mantiene el historial, métricas y trazabilidad de las ejecuciones de scraping/búsqueda realizadas por el usuario o script de Python.

---

## 🛠️ Configuración y Conexión

### Requisitos previos
- **PostgreSQL**: v15 o superior.
- **Base de Datos**: `repusense_db`

### Creación / Inicialización

1. Accede a PostgreSQL desde tu consola (`cmd`):
   ```bash
   "C:\Program Files\PostgreSQL\15\bin\psql.exe" -U postgres
   Crea la base de datos si aún no existe:

SQL
CREATE DATABASE repusense_db;
\c repusense_db
Ejecuta el script del esquema:

Bash
psql -U postgres -d repusense_db -f schema.sql
🔍 Comandos útiles en psql
Listar tablas: \dt

Ver estructura de una tabla: \d business

Consultar negocios: SELECT * FROM business;

Consultar leads: SELECT * FROM lead;

Consultar historial de búsquedas: SELECT * FROM search_run;

Salir: \q