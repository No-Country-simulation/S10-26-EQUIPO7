# 📋 Plan de Pruebas Maestro (Master Test Plan) - RepuSense

## 1. Introducción y Objetivos
El objetivo de este plan es validar la funcionalidad, integridad y usabilidad del sistema RepuSense (Backend Spring Boot y Frontend React), asegurando que el pipeline de captura de comercios con bajo rating, enriquecimiento con IA y gestión de leads cumpla con las reglas del negocio.

## 2. Alcance (In Scope)
- **Backend API:**
  - Validación de endpoints REST (`/api/v1/businesses`, `/api/v1/leads`).
  - Verificación de filtros por ciudad, estado y paginación.
  - Persistencia de estados comerciales (`NEW` a `CONTACTED`).
  - Validaciones de campos obligatorios y códigos de respuesta HTTP (200, 201, 400, 404).
- **Frontend Web:**
  - Carga inicial y conexión en vivo con PostgreSQL/Backend.
  - Filtros reactivos (Ubicación, Scoring de Riesgo, Problema Raíz).
  - Selección múltiple de leads y transición de estado a "Contactado".
  - Exportación de datos a CSV (completa y parcial).
  - Función de copiado al portapapeles de emails de contacto.

## 3. Fuera de Alcance (Out of Scope)
- Pruebas de carga extrema / estrés masivo (> 10.000 req/seg).
- Pruebas de compatibilidad con navegadores obsoletos (Internet Explorer).
- Automatización en dispositivos móviles nativos (iOS/Android).

## 4. Tipos de Pruebas a Realizar
1. **Pruebas de Humo (Smoke Tests):** Disponibilidad básica de la API y render del dashboard.
2. **Pruebas Funcionales y de Integración (API Testing):** Flujo de datos completo Backend-Base de Datos.
3. **Pruebas End-to-End (E2E / BDD):** Interacción del usuario en navegador web con Selenium y Behave.
4. **Pruebas de Regresión:** Ejecución automática en CI/CD ante cada nuevo push o pull request.

## 5. Estrategia y Ambientes de Prueba
- **Ambiente Local / QA:**
  - Backend: `http://localhost:8080/api/v1`
  - Frontend: `http://localhost:5173`
  - Base de Datos: PostgreSQL 14/18 (`repusense_db`)
- **Datos de Prueba (Test Data):** Comercios con rating < 3.5 y reviews >= 10 en Buenos Aires, Córdoba y Rosario.

## 6. Criterios de Entrada y Salida (Entry & Exit Criteria)
- **Criterios de Entrada:**
  - Build de Backend y Frontend exitosos sin errores de compilación.
  - Base de datos conectada y accesible.
- **Criterios de Salida:**
  - 100% de los Smoke Tests aprobados.
  - 0 defectos críticos (Bloqueantes / Sev-1) abiertos.
  - Mínimo 90% de casos automatizados en estado "Passed".

## 7. Gestión de Defectos y Herramientas
- **Gestión de Casos y Defectos:** GitHub Issues & GitHub Projects.
- **API Testing:** Apidog / Python Requests + Pytest.
- **Frontend E2E / BDD:** Python + Selenium WebDriver + Behave (Gherkin).
- **Reportes:** Allure Framework publicado en GitHub Actions / GitHub Pages.
