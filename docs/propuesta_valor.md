# Propuesta de Valor y Objetivo del Proyecto - RepuSense

## 📌 Objetivo Principal
Transformar datos de reputación pública en oportunidades de negocio concretas, identificando comercios con bajas puntuaciones o reseñas desatendidas en plataformas digitales y brindándoles soluciones automatizadas de diagnóstico y respuesta mediante Inteligencia Artificial (Gemini).

---

## 💥 El Problema
Muchos comercios locales sufren pérdida constante de clientes debido a:
- Puntuaciones bajas en Google Maps (< 3.8 estrellas).
- Reseñas negativas ignoradas o respondidas de forma inadecuada.
- Falta de tiempo o personal capacitado para gestionar la atención al cliente digital.

---

## 🚀 Soluciones Generadas con Inteligencia Artificial (Gemini API)

1. **Diagnóstico y Análisis de Sentimiento:**
   - Procesa y clasifica las reseñas en categorías de problemas recurrentes (atención al cliente, tiempos de demora, calidad de producto, precios).
2. **Generador de Respuestas Profesionales (*AI Assistant*):**
   - Produce borradores de respuestas empáticas, personalizadas y estratégicas listas para copiar, publicar y neutralizar el impacto negativo.
3. **Lead Scoring y Reportes Comerciales:**
   - Evalúa el nivel de urgencia/prioridad de cada comercio, generando un diagnóstico listo para ser utilizado por consultores o agencias en la venta de servicios de mejora de reputación.

---

## 🔄 Flujo de Trabajo del MVP
1. **Recolección/Captura:** Scraping o ingesta de comercios y sus reseñas.
2. **Filtrado:** Identificación de comercios con reputación crítica o sin respuesta.
3. **Almacenamiento:** Persistencia de registros (`Leads`) en PostgreSQL a través de Spring Boot.
4. **Procesamiento de IA:** Inspección del perfil y generación de diagnósticos/respuestas con la API de Gemini.
5. **Dashboard (React):** Visualización interactiva para gestionar los comercios y exportar las soluciones.