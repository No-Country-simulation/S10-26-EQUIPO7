import os
import sys
import re
import json
import requests
import pandas as pd
from dotenv import load_dotenv

# Asegurar compatibilidad de consola Windows con caracteres UTF-8
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# Cargar variables de entorno desde la raíz
load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api/v1")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "").strip()

def get_businesses():
    """Obtiene los comercios registrados en PostgreSQL vía Spring Boot."""
    url = f"{BACKEND_URL}/businesses"
    try:
        res = requests.get(url, timeout=10)
        if res.status_code == 200:
            return res.json()
        print(f"⚠️ Error al obtener comercios: {res.status_code}")
    except Exception as e:
        print(f"❌ Error conectando a {url}: {e}")
    return []

def get_existing_leads():
    """Obtiene los leads existentes para evitar duplicados."""
    url = f"{BACKEND_URL}/leads"
    try:
        res = requests.get(url, timeout=10)
        if res.status_code == 200:
            data = res.json()
            leads = data.get("content", data) if isinstance(data, dict) else data
            return {l.get("business", {}).get("id") for l in leads if l.get("business")}
    except Exception as e:
        print(f"⚠️ Error al consultar leads existentes: {e}")
    return set()

def load_reviews_dataset():
    """Carga las reseñas negativas desde el archivo CSV de muestra."""
    csv_path = "resenas_muestra_completa.csv"
    if not os.path.exists(csv_path):
        # Intentar en la carpeta padre
        csv_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "resenas_muestra_completa.csv")
    
    reviews_map = {}
    if os.path.exists(csv_path):
        try:
            df = pd.read_csv(csv_path)
            for _, row in df.iterrows():
                negocio = str(row.get("negocio", "")).strip().lower()
                revs = []
                for i in range(1, 6):
                    r_text = row.get(f"resena_{i}")
                    voto = row.get(f"voto_{i}")
                    if pd.notna(r_text) and str(r_text).strip():
                        revs.append(f"- ({voto}★) {str(r_text).strip()}")
                if negocio:
                    reviews_map[negocio] = "\n".join(revs)
        except Exception as e:
            print(f"⚠️ Error al leer CSV de reseñas: {e}")
    return reviews_map

def generate_ai_diagnosis_gemini(business, reviews_text):
    """Genera diagnóstico y propuesta de prospección utilizando Gemini."""
    name = business.get("name", "Comercio")
    category = business.get("category", "Local Comercial")
    city = business.get("city", "Buenos Aires")
    rating = business.get("rating", 2.5)

    prompt = f"""Eres un consultor experto en reputación online y marketing B2B para PyMEs locales.
Analiza este comercio con problemas reputacionales en Google Maps:
Negocio: {name}
Categoría: {category}
Ciudad: {city}
Calificación actual: {rating} estrellas.

Reseñas negativas recientes de clientes:
{reviews_text if reviews_text else 'Múltiples quejas por atención deficiente, demoras prolongadas y fallas en el servicio.'}

Genera un JSON con exactamente estos 3 campos:
1. "main_problem": Diagnóstico conciso en 1 frase indicando el problema dominante (especifica claramente si es sobre higiene, atención, demoras o precios).
2. "email_subject": Asunto profesional, empático y persuasivo para el dueño del negocio.
3. "email_opening": Primer párrafo de contacto en frío personalizado (2-3 oraciones) explicando el problema detectado en Google Maps y cómo nuestra solución le ayuda a subir el rating y recuperar clientes.

Responde ÚNICAMENTE con el objeto JSON:
{{
  "main_problem": "...",
  "email_subject": "...",
  "email_opening": "..."
}}
"""

    models = ["gemini-2.5-flash", "gemini-1.5-flash"]
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "responseMimeType": "application/json",
            "temperature": 0.2
        }
    }

    for model in models:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={GEMINI_API_KEY}"
        try:
            res = requests.post(url, json=payload, headers={"Content-Type": "application/json"}, timeout=20)
            if res.status_code == 200:
                text = res.json()["candidates"][0]["content"]["parts"][0]["text"]
                return json.loads(text)
        except Exception:
            continue

    # Fallback local en caso de que Gemini falle o agote cuota
    return generate_local_fallback_diagnosis(business, reviews_text)

def generate_local_fallback_diagnosis(business, reviews_text):
    """Diagnóstico local inteligente en caso de indisponibilidad de la API."""
    name = business.get("name", "Comercio")
    category = business.get("category", "Local")
    city = business.get("city", "Buenos Aires")
    rating = business.get("rating", 2.5)
    r_lower = (reviews_text or "").lower()

    if any(k in r_lower for k in ["higiene", "sucio", "olor", "guante", "infecc", "sanit"]):
        prob = "Grave riesgo sanitario y falta de higiene en las instalaciones."
    elif any(k in r_lower for k in ["demora", "espera", "tardo", "lento", "tiempo"]):
        prob = "Excesivos tiempos de espera y cuellos de botella en la atención."
    elif any(k in r_lower for k in ["precio", "cobro", "caro", "estafa", "tarifa"]):
        prob = "Disconformidad con tarifas elevadas y falta de transparencia en presupuestos."
    else:
        prob = "Problemas reiterados en la atención al cliente y trato displicente del personal."

    return {
        "main_problem": prob,
        "email_subject": f"Propuesta estratégica para revertir reseñas negativas en {name}",
        "email_opening": f"Hola equipo de {name}, detectamos que actualmente cuentan con una calificación de {rating}★ en Google Maps en {city}. Desarrollamos un plan de remediación reputacional diseñado para resolver estas quejas, mejorar el posicionamiento y atraer nuevos clientes."
    }

def generate_contact_email(business):
    """Genera un correo de contacto creíble a partir del sitio web o nombre."""
    website = business.get("website", "")
    if website and "http" in website:
        domain = re.sub(r"^https?://(www\.)?", "", website).split("/")[0]
        return f"contacto@{domain}"
    
    clean_name = re.sub(r"[^a-zA-Z0-9]", "", business.get("name", "comercio")).lower()
    return f"contacto@{clean_name}.com"

def save_lead_to_backend(business_id, email, ai_data):
    """Guarda el Lead generado en PostgreSQL mediante Spring Boot."""
    url = f"{BACKEND_URL}/leads"
    payload = {
        "business": {"id": business_id},
        "status": "NEW",
        "contactEmail": email,
        "aiMainProblem": ai_data.get("main_problem"),
        "aiEmailSubject": ai_data.get("email_subject"),
        "aiEmailOpening": ai_data.get("email_opening")
    }

    try:
        res = requests.post(url, json=payload, headers={"Content-Type": "application/json"}, timeout=10)
        if res.status_code in [200, 201]:
            return res.json()
        print(f"⚠️ Error al guardar lead (ID {business_id}): {res.status_code} - {res.text}")
    except Exception as e:
        print(f"❌ Error al enviar lead al backend: {e}")
    return None

def run_lead_seeder():
    print("=" * 65)
    print("🚀 REPUSENSE - Generador de Leads y Diagnósticos con IA (Gemini)")
    print("=" * 65)

    businesses = get_businesses()
    if not businesses:
        print("❌ No se encontraron comercios en la base de datos (puerto 8080).")
        return

    print(f"📍 Comercios encontrados en PostgreSQL: {len(businesses)}")
    
    existing_lead_ids = get_existing_leads()
    print(f"📋 Leads ya existentes en la BD: {len(existing_lead_ids)}")

    reviews_map = load_reviews_dataset()
    print(f"📖 Reseñas cargadas del CSV para cotejo: {len(reviews_map)} comercios\n")

    creados = 0
    for b in businesses:
        b_id = b.get("id")
        name = b.get("name", "Sin nombre")

        if b_id in existing_lead_ids:
            print(f"⏭️  [Omitido] '{name}' (ID: {b_id}) ya posee un lead generado.")
            continue

        print(f"🤖 Procesando con IA: {name} (ID: {b_id})...")
        
        # Buscar reseñas correspondientes
        r_text = reviews_map.get(name.lower().strip(), "")
        if not r_text:
            # Búsqueda parcial si hay diferencias de acentos o puntuación
            for k, v in reviews_map.items():
                if k in name.lower() or name.lower() in k:
                    r_text = v
                    break

        # Llamar a Gemini (con fallback inteligente)
        ai_data = generate_ai_diagnosis_gemini(b, r_text)
        email = generate_contact_email(b)

        print(f"   💡 Problema: {ai_data.get('main_problem')}")
        print(f"   📧 Asunto: {ai_data.get('email_subject')}")

        saved = save_lead_to_backend(b_id, email, ai_data)
        if saved:
            print(f"   ✅ Lead guardado exitosamente en PostgreSQL (ID: {saved.get('id')})!\n")
            creados += 1

    print("=" * 65)
    print(f"🎉 ¡Proceso finalizado! Se crearon {creados} leads con IA en PostgreSQL.")
    print("=" * 65)

if __name__ == "__main__":
    run_lead_seeder()