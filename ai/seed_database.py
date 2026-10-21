import pandas as pd
import requests

BACKEND_URL = "http://localhost:8080/api/v1/businesses"

def inferir_rubro(nombre: str) -> str:
    """Infiere el rubro/categoría automáticamente a partir del nombre del comercio."""
    n = nombre.lower()
    if any(k in n for k in ["dental", "clínica", "clinica", "odontol"]):
        return "Salud / Clínica Dental"
    elif any(k in n for k in ["restaurante", "pizza", "burger", "café", "cafe", "hamburgues"]):
        return "Gastronomía / Cafetería"
    elif any(k in n for k in ["taller", "mecánic", "mecanic", "pistón", "auto"]):
        return "Servicios / Mecánica"
    elif any(k in n for k in ["estética", "estetica", "spa", "belleza", "peluquer"]):
        return "Salud / Estética"
    elif any(k in n for k in ["veterinaria", "huellitas", "mascota", "animal"]):
        return "Salud / Veterinaria"
    return "Comercio Minorista / Retail"

def run_seeder():
    archivo_csv = "resenas_muestra_completa.csv"
    print(f"📖 Leyendo comercios desde {archivo_csv}...")
    
    try:
        df = pd.read_csv(archivo_csv)
    except Exception as e:
        print(f"❌ Error al abrir el CSV: {e}")
        return

    insertados = 0

    for idx, row in df.iterrows():
        nombre = str(row.get("negocio", f"Comercio #{idx+1}")).strip()
        ciudad = str(row.get("ciudad", "Buenos Aires")).strip()
        pais = str(row.get("pais", "Argentina")).strip()
        telefono = str(row.get("telefono", "+54 11 4444-5555")).strip()
        website = str(row.get("web", "https://ejemplo.com")).strip()

        # Inferencia inteligente de rubro/categoría
        rubro = inferir_rubro(nombre)

        # Calcular el promedio de estrellas a partir de voto_1 a voto_5
        votos = []
        for i in range(1, 6):
            v = row.get(f"voto_{i}")
            if pd.notna(v):
                try:
                    votos.append(float(v))
                except ValueError:
                    pass

        rating_promedio = round(sum(votos) / len(votos), 2) if votos else 2.5

        # Armado del payload con todos los campos requeridos por PostgreSQL (NOT NULL)
        payload = {
            "source": "GOOGLE_MAPS",
            "sourceBusinessId": f"MOCK_PLACE_{idx + 101}",
            "name": nombre,
            "city": f"{ciudad}, {pais}" if pais else ciudad,
            "category": rubro,
            "rating": rating_promedio,
            "reviewCount": len(votos) * 5,
            "phone": telefono,
            "website": website
        }

        try:
            resp = requests.post(BACKEND_URL, json=payload, headers={"Content-Type": "application/json"})
            if resp.status_code in [200, 201]:
                print(f"✅ Guardado [{resp.status_code}]: {nombre} | Rubro: {rubro} | Rating: {rating_promedio} ⭐")
                insertados += 1
            else:
                print(f"⚠️ Error al guardar '{nombre}': {resp.status_code} - {resp.text}")
        except Exception as err:
            print(f"❌ No se pudo conectar al Backend (¿está corriendo en puerto 8080?): {err}")
            return

    print(f"\n🎉 ¡Proceso completado! Se insertaron {insertados} comercios exitosamente en PostgreSQL.")

if __name__ == "__main__":
    run_seeder()
