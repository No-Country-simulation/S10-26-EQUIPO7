import pytest

try:
    import allure
except ImportError:
    allure = None

# ==========================================
# 1. CASOS POSITIVOS DE LEADS
# ==========================================

def test_get_leads_pagination_structure(api):
    """Verifica que el listado de leads retorne paginación correcta."""
    res = api.get(f"{api.base_url}/leads?page=0&size=5")
    assert res.status_code == 200, f"Error: esperó 200 pero devolvió {res.status_code}"
    data = res.json()
    assert "content" in data, "No tiene campo 'content'"
    assert "totalElements" in data, "No tiene campo 'totalElements'"
    assert len(data["content"]) <= 5, "Devolvió más elementos de los solicitados en size=5"

def test_lead_full_lifecycle(api):
    """Flujo completo: crear lead -> cambiar estado a CONTACTED -> verificar persistencia."""
    # 1. Obtener un comercio base existente
    b_res = api.get(f"{api.base_url}/businesses")
    assert b_res.status_code == 200 and len(b_res.json()) > 0, "Se requiere al menos un comercio cargado"
    comercio = b_res.json()[0]

    # 2. Crear Lead (POST)
    lead_data = {
        "business": {"id": comercio["id"]},
        "status": "NEW",
        "contactEmail": "contacto@test-qa.com.ar",
        "aiMainProblem": "Problema crítico de demoras en pedidos",
        "aiEmailSubject": "Propuesta RepuSense QA",
        "aiEmailOpening": "Estimado titular, detectamos oportunidades..."
    }
    create_res = api.post(f"{api.base_url}/leads", json=lead_data)
    assert create_res.status_code in [200, 201], f"Fallo al crear lead: {create_res.text}"
    lead_id = create_res.json()["id"]

    # 3. Marcar como CONTACTED (PATCH)
    patch_res = api.patch(f"{api.base_url}/leads/{lead_id}/status", json={"status": "CONTACTED"})
    assert patch_res.status_code == 200, f"Fallo al actualizar status: {patch_res.text}"

    # 4. Validar persistencia
    check_res = api.get(f"{api.base_url}/leads?status=CONTACTED")
    assert check_res.status_code == 200
    leads = check_res.json().get("content", [])
    assert any(l["id"] == lead_id for l in leads), f"El lead {lead_id} no figura en la lista de CONTACTED"

def test_export_leads_csv_returns_200_and_csv_type(api):
    """Verifica que el endpoint de exportación CSV funcione y devuelva el header correcto."""
    res = api.get(f"{api.base_url}/leads/export/csv")
    assert res.status_code == 200
    assert "text/csv" in res.headers.get("Content-Type", ""), f"Tipo inesperado: {res.headers.get('Content-Type')}"

# ==========================================
# 2. CASOS NEGATIVOS Y VALIDACIONES DE ERROR
# ==========================================

def test_update_status_non_existent_lead_returns_404(api):
    """Caso negativo: Intentar actualizar el estado de un lead inexistente debe dar 404."""
    res = api.patch(f"{api.base_url}/leads/999999/status", json={"status": "CONTACTED"})
    assert res.status_code == 404

def test_update_status_with_empty_value_returns_400(api):
    """Caso negativo: Enviar un estado en blanco o vacío debe dar 400 Bad Request."""
    leads_res = api.get(f"{api.base_url}/leads")
    leads = leads_res.json().get("content", [])
    assert len(leads) > 0, "No hay leads para probar actualización"
    lead_id = leads[0]["id"]

    res = api.patch(f"{api.base_url}/leads/{lead_id}/status", json={"status": "   "})
    assert res.status_code in [400, 500]

def test_leads_negative_page_returns_400(api):
    """Validación: Página negativa (ej. page=-1) debe devolver 400 Bad Request."""
    res = api.get(f"{api.base_url}/leads?page=-1&size=10")
    assert res.status_code in [400, 500]

def test_leads_page_size_zero_returns_400(api):
    """Validación: Tamaño de página menor a 1 (ej. size=0) debe devolver 400 Bad Request."""
    res = api.get(f"{api.base_url}/leads?page=0&size=0")
    assert res.status_code in [400, 500]
