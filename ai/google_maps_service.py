
import os
import requests
from dotenv import load_dotenv

load_dotenv()

GOOGLE_PLACES_API_KEY = os.getenv("GOOGLE_PLACES_API_KEY")

PLACES_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json"


def search_businesses_in_maps(rubro="Pizzería", ciudad="La Plata"):
    if not GOOGLE_PLACES_API_KEY:
        print("ERROR: falta GOOGLE_PLACES_API_KEY en el archivo .env")
        return []

    query = f"{rubro} en {ciudad}"
    params = {
        "query": query,
        "key": GOOGLE_PLACES_API_KEY,
        "language": "es",
        "region": "ar"
    }

    print(f"Buscando comercios en Google Places: {query}")

    try:
        response = requests.get(
            PLACES_URL,
            params=params,
            timeout=20
        )
        response.raise_for_status()
        data = response.json()

        status = data.get("status")

        if status != "OK":
            print(f"Google Places devolvió: {status}")
            if data.get("error_message"):
                print(data["error_message"])
            return []

        businesses = []

        for place in data.get("results", []):
            businesses.append({
                "source": "GOOGLE_MAPS",
                "sourceBusinessId": place.get("place_id"),
                "name": place.get("name"),
                "category": rubro.strip().capitalize(),
                "city": ciudad.strip(),
                "address": place.get("formatted_address"),
                "rating": place.get("rating", 0.0),
                "reviewCount": place.get("user_ratings_total", 0),
                "phone": None,
                "website": None
            })

        print(f"Se encontraron {len(businesses)} comercios.")
        return businesses

    except requests.RequestException as error:
        print(f"Error consultando Google Places: {error}")
        return []


if __name__ == "__main__":
    resultados = search_businesses_in_maps("Farmacia", "La Plata")

    for comercio in resultados[:5]:
        print(
            f"- {comercio['name']} | "
            f"{comercio['rating']} estrellas | "
            f"{comercio['address']}"
        )
