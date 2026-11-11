import os
import json
import pytest
import requests

try:
    import allure
except ImportError:
    allure = None

BASE_URL = "http://localhost:8080"

@pytest.fixture(scope="session")
def api():
    """
    Fixture de sesión:
    1. Intenta iniciar sesión con el usuario de QA.
    2. Si el usuario no existe (BD limpia), lo registra automáticamente.
    3. Obtiene el JWT token fresco.
    4. Configura el Header 'Authorization: Bearer <token>' para toda la suite.
    5. Intercepta todas las llamadas para registrar evidencia en logs y en Allure.
    """
    session = requests.Session()

    usuario_qa = {
        "name": "QA Automation Tester",
        "email": "qa.tester@repusense.com",
        "password": "Password123!"
    }

    # 1. Intentar Login
    login_url = f"{BASE_URL}/api/auth/login"
    res_login = session.post(login_url, json={
        "email": usuario_qa["email"],
        "password": usuario_qa["password"]
    })

    # 2. Si no existe (código 401 o 400), registrarlo primero
    if res_login.status_code != 200:
        session.post(f"{BASE_URL}/api/auth/register", json=usuario_qa)
        res_login = session.post(login_url, json={
            "email": usuario_qa["email"],
            "password": usuario_qa["password"]
        })

    assert res_login.status_code == 200, f"Error crítico: No se pudo autenticar en la API: {res_login.text}"

    # 3. Extraer el token
    token = res_login.json().get("token")
    assert token, "La respuesta de login no incluyó el JWT token"

    # 4. Inyectar el token en la sesión para todos los tests futuros
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    })
    session.base_url = f"{BASE_URL}/api/v1"

    # 5. Wrapper para capturar automáticamente REQUEST y RESPONSE como evidencia
    original_request = session.request

    def logging_request(method, url, **kwargs):
        print(f"\n➡️ [REQUEST] {method.upper()} {url}")
        if "json" in kwargs:
            print(f"📦 [PAYLOAD]: {json.dumps(kwargs['json'], indent=2, ensure_ascii=False)}")

        response = original_request(method, url, **kwargs)

        print(f"⬅️ [RESPONSE] Status: {response.status_code} ({response.elapsed.total_seconds():.3f}s)")
        try:
            body_json = response.json()
            print(f"📄 [BODY]: {json.dumps(body_json, indent=2, ensure_ascii=False)}")
        except Exception:
            print(f"📄 [BODY]: {response.text}")

        # Evidencia adjunta para Allure Report
        if allure:
            with allure.step(f"HTTP {method.upper()} {url} -> {response.status_code}"):
                if "json" in kwargs:
                    allure.attach(
                        json.dumps(kwargs["json"], indent=2, ensure_ascii=False),
                        name="Request JSON",
                        attachment_type=allure.attachment_type.JSON
                    )
                allure.attach(
                    response.text,
                    name=f"Response {response.status_code}",
                    attachment_type=allure.attachment_type.JSON if "application/json" in response.headers.get("Content-Type", "") else allure.attachment_type.TEXT
                )

        return response

    session.request = logging_request

    return session
