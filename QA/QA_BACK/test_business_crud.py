import pytest

try:
    import allure
except ImportError:
    allure = None

# ==========================================
# CASOS POSITIVOS
# ==========================================

def test_get_all_businesses_returns_200(api):
    """Comprueba que un usuario con JWT válido pueda consultar todos los comercios."""
    res = api.get(f"{api.base_url}/businesses")
    assert res.status_code == 200, f"Error: esperó 200 pero devolvió {res.status_code}"
    assert isinstance(res.json(), list), "La respuesta no es una lista de comercios"

def test_create_and_delete_business_lifecycle(api):
    """Ciclo de vida: Crea un comercio, verifica su persistencia y luego lo elimina."""
    payload = {
        "source": "QA_AUTOMATION",
        "sourceBusinessId": "QA_ID_9999",
        "name": "Cafetería Test JWT",
        "city": "Cordoba",
        "category": "Cafetería",
        "rating": 3.1,
        "reviewCount": 18
    }

    # Crear (POST)
    post_res = api.post(f"{api.base_url}/businesses", json=payload)
    assert post_res.status_code in [200, 201], f"Fallo al crear: {post_res.text}"
    created_id = post_res.json()["id"]

    # Consultar por ID (GET)
    get_res = api.get(f"{api.base_url}/businesses/{created_id}")
    assert get_res.status_code == 200
    assert get_res.json()["name"] == "Cafetería Test JWT"

    # Eliminar (DELETE)
    del_res = api.delete(f"{api.base_url}/businesses/{created_id}")
    assert del_res.status_code in [200, 204]

    # Verificar que ya no existe (GET 404)
    verify_res = api.get(f"{api.base_url}/businesses/{created_id}")
    assert verify_res.status_code == 404

# ==========================================
# CASOS NEGATIVOS (Validaciones de Error)
# ==========================================

def test_get_business_non_existent_id_returns_404(api):
    """Validación: Consultar un comercio que no existe debe dar 404 Not Found."""
    res = api.get(f"{api.base_url}/businesses/999999")
    assert res.status_code == 404

def test_update_business_non_existent_id_returns_404(api):
    """Validación: Actualizar un comercio que no existe debe dar 404."""
    payload = {"name": "Inexistente", "city": "Rosario", "category": "Bar"}
    res = api.put(f"{api.base_url}/businesses/999999", json=payload)
    assert res.status_code == 404

def test_delete_business_non_existent_id_returns_404(api):
    """Validación: Eliminar un ID inexistente debe responder 404."""
    res = api.delete(f"{api.base_url}/businesses/999999")
    assert res.status_code == 404
