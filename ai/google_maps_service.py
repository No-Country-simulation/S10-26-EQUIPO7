import os
import requests
from dotenv import load_dotenv

load_dotenv()

# Usa la clave de API de Google / Places configurada en el .env
GOOGLE_MAPS_API_KEY = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_PLACES_API_KEY")

def search_businesses_in_maps(rubro="Pizzería", ciudad="La Plata"):
    """
    Realiza una búsqueda REAL en Google Maps/Places API.
    Devuelve una lista de comercios reales con su nombre, rating, dirección y categoría.
    """
    query = f"{rubro} en {ciudad}"
    url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
    
    params = {
        "query": query,
        "key": GOOGLE_MAPS_API_KEY
    }
    
    print(f"📡 Buscando en Google Maps: '{query}'...")
    
    try:
        response = requests.get(url, params=params)
        data = response.json()
        
        status = data.get("status")
        if status != "OK":
            print(f"⚠️ Google Maps API devolvió estado: {status}")
            if "error_message" in data:
                print(f"   Detalle: {data['error_message']}")
            return []

        results = data.get("results", [])
        businesses = []

        for place in results:
            # Extraer ciudad y dirección limpia si está disponible
            formatted_address = place.get("formatted_address", f"{ciudad}, Argentina")
            
            businesses.append({
                "name": place.get("name"),
                "category": rubro.capitalize(),
                "city": ciudad,
                "rating": place.get("rating", 3.5),
                "address": formatted_address
            })

        print(f"✅ Se encontraron {len(businesses)} comercios reales en Google Maps.")
        return businesses

    except Exception as e:
        print(f"❌ Error conectando con Google Maps API: {e}")
        return []

if __name__ == "__main__":
    # Prueba rápida independiente
    res = search_businesses_in_maps("Pizzería", "La Plata")
    for b in res[:3]:
        print(f" - {b['name']} ({b['rating']} ⭐) | {b['address']}")