

"""
REPUSENSE - GENERATE LEADS

Módulo antiguo de generación de leads mediante Gemini y CSV.

La arquitectura actual centraliza el análisis de reputación
en Spring Boot, utilizando OpenAI y las reseñas asociadas
a cada negocio en PostgreSQL.

Este archivo queda desactivado para evitar:
- Uso de Gemini.
- Lectura de reseñas ficticias desde CSV.
- Asociación incorrecta de reseñas por nombre.
- Generación de correos electrónicos inventados.
- Inserciones de leads mediante endpoints no confirmados.

No modifica ni elimina registros de PostgreSQL.
"""

import requests

from config import BACKEND_URL, REQUEST_TIMEOUT


def analyze_existing_lead(lead_id):
    """
    Solicita al backend el análisis de un lead existente.

    El backend utiliza OpenAI y los datos disponibles
    en PostgreSQL para realizar el diagnóstico.
    """

    if not lead_id:
        print("Error: se necesita un ID de lead.")
        return None

    url = f"{BACKEND_URL}/leads/{lead_id}/analyze"

    try:
        response = requests.post(
            url,
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code in (200, 201):
            print(f"Análisis completado para el lead {lead_id}.")
            return response.json()

        print(
            f"Error al analizar el lead {lead_id}: "
            f"HTTP {response.status_code}"
        )
        print(response.text)

    except requests.RequestException as error:
        print(f"Error de conexión con el backend: {error}")

    return None


def run_lead_seeder():
    """
    Desactiva el antiguo proceso de generación masiva.

    Los leads existentes se analizan desde el backend.
    """

    print("=" * 60)
    print("REPUSENSE - GENERACIÓN DE LEADS")
    print("=" * 60)

    print("\nEl generador antiguo quedó desactivado.")
    print("No se utilizará Gemini ni el CSV de reseñas de muestra.")
    print("No se crearán correos electrónicos ficticios.")
    print("No se insertarán leads duplicados.")

    print(
        "\nPara analizar un lead existente, "
        "utilizá el endpoint de análisis de Spring Boot."
    )

    print("\nNo se modificó PostgreSQL.")


if __name__ == "__main__":
    run_lead_seeder()
