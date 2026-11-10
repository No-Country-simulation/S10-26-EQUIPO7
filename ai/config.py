
import os
from pathlib import Path
from dotenv import load_dotenv

# Cargar variables del archivo .env ubicado junto a config.py
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

# Backend Spring Boot
BACKEND_URL = os.getenv(
    "BACKEND_URL",
    "http://localhost:8080/api/v1"
).rstrip("/")

# SerpApi
SERPAPI_API_KEY = os.getenv("SERPAPI_API_KEY", "").strip()

# OpenAI
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()

# Reputación: rating igual o inferior a 3.5
MAX_RATING_THRESHOLD = 3.5

# Configuración general
REQUEST_TIMEOUT = 30


def validate_configuration():
    """Comprueba las variables necesarias sin mostrar claves."""

    missing = []

    if not BACKEND_URL:
        missing.append("BACKEND_URL")

    if not SERPAPI_API_KEY:
        missing.append("SERPAPI_API_KEY")

    if not OPENAI_API_KEY:
        missing.append("OPENAI_API_KEY")

    if missing:
        raise RuntimeError(
            "Faltan variables de entorno: " + ", ".join(missing)
        )
