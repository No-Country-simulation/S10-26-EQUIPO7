import os

# URL del backend en Spring Boot
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api/v1")

# Umbral de estrellas para clasificar como lead con oportunidad de mejora
MAX_RATING_THRESHOLD = 3.5