import sys
import os
import requests
from dotenv import load_dotenv
from google_maps_service import search_businesses_in_maps
from generate_leads import generate_lead_local_ai, save_lead_to_backend
from config import BACKEND_URL

load_dotenv()

def get_existing_lead_business_ids():
    """Obtiene los IDs de comercios que ya tienen un Lead asignado en la BD."""
    url = f"{BACKEND_URL}/leads"
    try:
        response = requests.get(url)
        if response.status_code == 200:
            data = response.json()
            leads = data.get('content', []) if isinstance(data, dict) else data
            return {lead['business']['id'] for lead in leads if isinstance(lead, dict) and lead.get('business')}
    except Exception as e:
        print(f"⚠️ No se pudieron consultar los leads existentes: {e}")
    return set()

def save_business_to_backend(business_data):
    """Guarda un comercio en PostgreSQL a través de Spring Boot."""
    url = f"{BACKEND_URL}/businesses"
    try:
        res = requests.post(url, json=business_data)
        if res.status_code in [200, 201]:
            created = res.json()
            print(f"💾 Guardado en BD: {created.get('name')} (ID: {created.get('id')})")
            return created
        else:
            print(f"⚠️ No se pudo guardar '{business_data.get('name')}': {res.status_code}")
    except Exception as e:
        print(f"❌ Error al enviar comercio al backend: {e}")
    return None

def run_real_discovery(rubro="Pizzería", ciudad="La Plata"):
    print(f"\n🚀 === INICIANDO REPUSENSE SCOUTING REAL ===")
    print(f"📍 Ciudad: {ciudad} | 🏷️ Rubro: {rubro}\n")

    # 1. Búsqueda Real en Google Maps
    real_places = search_businesses_in_maps(rubro, ciudad)
    if not real_places:
        print("❌ No se obtuvieron resultados de Google Maps. Revisa tu clave de API o la conexión.")
        return

    # 2. Obtener IDs de comercios que ya tienen lead
    existing_lead_bids = get_existing_lead_business_ids()

    # 3. Guardar comercios y generar sus diagnósticos
    for place in real_places:
        saved_b = save_business_to_backend(place)
        
        if saved_b and saved_b.get("id"):
            b_id = saved_b.get("id")
            
            if b_id in existing_lead_bids:
                print(f"ℹ️ El comercio '{saved_b.get('name')}' ya tiene un Lead activo. Omitiendo...")
                continue
                
            print(f"⚡ Generando propuesta de IA para: {saved_b.get('name')}...")
            ai_data = generate_lead_local_ai(saved_b)
            save_lead_to_backend(b_id, ai_data)

    print(f"\n🎉 === SCOUTING FINALIZADO ===")

if __name__ == "__main__":
    arg_rubro = sys.argv[1] if len(sys.argv) > 1 else "Pizzería"
    arg_ciudad = sys.argv[2] if len(sys.argv) > 2 else "La Plata"
    
    run_real_discovery(arg_rubro, arg_ciudad)