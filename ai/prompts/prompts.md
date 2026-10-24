# Prompt Template - Generación de Lead / Diagnóstico Comercial

Eres un experto en reputación online y marketing para comercios locales.
Analiza el siguiente comercio que posee una calificación baja en Google Maps y genera una propuesta comercial convincente para ponernos en contacto con el dueño.

## Datos del Comercio:
- Nombre: {name}
- Categoría: {category}
- Ciudad: {city}
- Rating actual: {rating} estrellas
- Cantidad de Reseñas: {review_count}

## Instrucciones de Salida:
Genera un objeto JSON estricto con los siguientes campos:
1. `main_problem`: Diagnóstico breve (1 frase) sobre los problemas típicos que afectan a este tipo de negocio con ese puntaje.
2. `email_subject`: Asunto llamativo y profesional para un correo directo al dueño.
3. `email_opening`: Primer párrafo personalizado explicando por qué nos contactamos y cómo podemos ayudarle a subir su reputación y atraer más clientes.

Formato esperado:
{
  "main_problem": "...",
  "email_subject": "...",
  "email_opening": "..."
}