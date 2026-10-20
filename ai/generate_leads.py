import os
import json
import requests
from dotenv import load_dotenv
from config import BACKEND_URL

load_dotenv()

def get_unprocessed_businesses():
    """Obtiene la lista de negocios registrados en la BD."""
    url = f"{BACKEND_URL}/businesses"
    try:
        response = requests.get(url)
        if response.status_code == 200:
            return response.json()
    except Exception as e:
        print(f"❌ Error al consultar comercios: {e}")
    return []

def generate_lead_local_ai(business):
    """Genera el diagnóstico comercial ajustado al negocio."""
    name = business.get('name', 'Comercio')
    category = business.get('category', 'Local Comercial')
    city = business.get('city', 'La Plata')
    rating = business.get('rating', 3.0)

    # Diagnóstico automático basado en categoría y calificación
    return {
        "main_problem": f"Baja calificación promedio ({rating} estrellas) en Google Maps dificulta la captación de nuevos clientes en {city}.",
        "email_subject": f"Propuesta para impulsar la reputación online de {name}",
        "email_opening": f"Hola equipo de {name}, notamos que actualmente tienen una puntuación de {rating} en Google Maps. Diseñamos una estrategia automatizada para optimizar sus reseñas, responder a las críticas de forma profesional y atraer más clientes locales en {city}."
    }

def save_lead_to_backend(business_id, ai_data):
    """Guarda el Lead generado en el Backend."""
    url = f"{BACKEND_URL}/leads"
    payload = {
        "business": {"id": business_id},
        "status": "NEW",
        "aiMainProblem": ai_data.get("main_problem"),
        "aiEmailSubject": ai_data.get("email_subject"),
        "aiEmailOpening": ai_data.get("email_opening")
    }
    
    try:
        res = requests.post(url, json=payload)
        if res.status_code in [200, 201]:
            print(f"🎯 Lead creado exitosamente en la base de datos para ID negocio: {business_id}")
            return res.json()
        else:
            print(f"⚠️ Error al guardar lead en backend: {res.status_code} - {res.text}")
    except Exception as e:
        print(f"❌ Error al enviar lead al backend: {e}")
    return None

def process_leads():
    businesses = get_unprocessed_businesses()
    print(f"🔍 Encontrados {len(businesses)} negocios en la base de datos.")

    for b in businesses:
        b_id = b.get("id")
        name = b.get("name")
        print(f"\n⚡ Generando diagnóstico comercial para: {name} (ID: {b_id})...")
        
        ai_data = generate_lead_local_ai(b)
        print(f"💡 Problema detectado: {ai_data.get('main_problem')}")
        print(f"📧 Asunto: {ai_data.get('email_subject')}")
        save_lead_to_backend(b_id, ai_data)

if __name__ == "__main__":
    process_leads()