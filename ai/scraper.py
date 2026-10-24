import requests
import json
from config import BACKEND_URL, MAX_RATING_THRESHOLD

def send_business_to_backend(business_data):
    """Envia un comercio extraido al backend de Spring Boot."""
    url = f"{BACKEND_URL}/businesses"
    headers = {"Content-Type": "application/json"}
    
    try:
        response = requests.post(url, json=business_data, headers=headers)
        if response.status_code in [200, 201]:
            print(f"✅ Guardado con éxito: {business_data['name']} (Rating: {business_data['rating']})")
            return response.json()
        else:
            print(f"⚠️ Error al guardar {business_data['name']}: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"❌ No se pudo conectar con el backend: {e}")
    return None

def process_extracted_places(places):
    """Filtra comercios con bajo puntaje y los persiste."""
    print(f"\n🔍 Procesando {len(places)} comercios encontrados...")
    
    saved_count = 0
    for place in places:
        rating = place.get("rating", 5.0)
        
        # Filtro clave: Solo procesar negocios con baja reputacion
        if rating <= MAX_RATING_THRESHOLD:
            payload = {
                "source": "google_maps",
                "sourceBusinessId": place.get("place_id"),
                "name": place.get("name"),
                "city": place.get("city", "La Plata"),
                "category": place.get("category", "Restaurante"),
                "rating": rating,
                "reviewCount": place.get("user_ratings_total", 0),
                "phone": place.get("phone", "N/A"),
                "website": place.get("website", "N/A")
            }
            result = send_business_to_backend(payload)
            if result:
                saved_count += 1
        else:
            print(f"⏭️ Omitido: {place['name']} (Rating alto: {rating})")

    print(f"\n✨ Resumen: {saved_count} comercios guardados en la base de datos.")

if __name__ == "__main__":
    # Ejemplo de datos extraidos (Simulacion de respuesta de Google Maps)
    sample_places = [
        {
            "place_id": "ChIJ_12345",
            "name": "Pizzería Central",
            "city": "La Plata",
            "category": "Pizzería",
            "rating": 3.2,
            "user_ratings_total": 45,
            "phone": "+542211234567",
            "website": "http://pizzeriacentral.com"
        },
        {
            "place_id": "ChIJ_67890",
            "name": "Café Gourmet",
            "city": "La Plata",
            "category": "Cafetería",
            "rating": 4.8,
            "user_ratings_total": 120,
            "phone": "+542219876543",
            "website": "http://cafegourmet.com"
        }
    ]

    process_extracted_places(sample_places)