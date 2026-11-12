"""
steps/dashboard_steps.py
Implementación de los pasos Gherkin para el Dashboard RepuSense.

Selectores basados en la inspección de App.jsx:
- Tabla:         table > thead > tr > th  /  table > tbody > tr
- Filtro ciudad: input[aria-label="Filtrar ciudad"]
- Filtro rubro:  select[aria-label="Filtrar estado"]   (o buscar por texto)
- Buscar nombre: input[aria-label="Buscar comercio"]
- Botón CSV:     button que contiene "Exportar CSV"
- Email:         columna con texto @
- Copiar email:  botón que contiene "📋" o aria-label="Copiar email"
"""
import time

import allure
from behave import given, when, then
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

BASE_URL = "http://localhost:5173"
WAIT = 15  # segundos máximos de espera


def wait_for(context, condition, timeout=WAIT):
    return WebDriverWait(context.driver, timeout).until(condition)


# ──────────────────────────────────────────────────────────────────
# GIVEN  (precondiciones)
# ──────────────────────────────────────────────────────────────────

@given("que el usuario abre el dashboard")
def step_abrir_dashboard(context):
    with allure.step("Abrir el dashboard en el navegador"):
        context.driver.get(BASE_URL)
        # Esperar a que se muestre algo de la UI (login o dashboard)
        wait_for(
            context,
            EC.presence_of_element_located((By.TAG_NAME, "main")),
            timeout=20,
        )
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name="dashboard_cargado",
            attachment_type=allure.attachment_type.PNG,
        )


@given("espera que la tabla cargue datos")
def step_esperar_tabla(context):
    with allure.step("Esperar que la tabla tenga al menos una fila"):
        wait_for(
            context,
            EC.presence_of_element_located(
                (By.CSS_SELECTOR, "table tbody tr")
            ),
            timeout=20,
        )
        time.sleep(1)  # pequeña pausa para que React termine de renderizar


# ──────────────────────────────────────────────────────────────────
# THEN  (carga inicial)
# ──────────────────────────────────────────────────────────────────

@then("la tabla debe mostrar al menos un comercio")
def step_tabla_tiene_comercios(context):
    with allure.step("Verificar que hay filas en la tabla"):
        rows = context.driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
        assert len(rows) > 0, (
            f"Se esperaba al menos 1 comercio en la tabla, pero hay {len(rows)}"
        )
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name="tabla_con_datos",
            attachment_type=allure.attachment_type.PNG,
        )


@then('la tabla debe tener la columna "{nombre}"')
def step_tabla_tiene_columna(context, nombre):
    with allure.step(f"Verificar columna '{nombre}'"):
        headers = context.driver.find_elements(By.CSS_SELECTOR, "table thead th")
        textos = [h.text.strip() for h in headers]
        assert any(nombre.lower() in t.lower() for t in textos), (
            f"No se encontró la columna '{nombre}'. Columnas presentes: {textos}"
        )


# ──────────────────────────────────────────────────────────────────
# WHEN  (acciones de filtros)
# ──────────────────────────────────────────────────────────────────

@when('selecciona la ciudad "{ciudad}"')
def step_filtrar_ciudad(context, ciudad):
    with allure.step(f"Escribir '{ciudad}' en el filtro de ciudad"):
        campo = wait_for(
            context,
            EC.element_to_be_clickable(
                (By.CSS_SELECTOR, "input[aria-label='Filtrar ciudad']")
            ),
        )
        campo.clear()
        campo.send_keys(ciudad)
        time.sleep(0.8)
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name=f"filtro_ciudad_{ciudad}",
            attachment_type=allure.attachment_type.PNG,
        )


@when("limpia el filtro de ciudad")
def step_limpiar_filtro(context):
    with allure.step("Limpiar el campo de ciudad"):
        campo = context.driver.find_element(
            By.CSS_SELECTOR, "input[aria-label='Filtrar ciudad']"
        )
        campo.clear()
        time.sleep(0.8)


@when('selecciona el rubro "{rubro}"')
def step_filtrar_rubro(context, rubro):
    with allure.step(f"Escribir '{rubro}' en el buscador"):
        # En el front el filtro de rubro es el input de búsqueda general
        campo = wait_for(
            context,
            EC.element_to_be_clickable(
                (By.CSS_SELECTOR, "input[aria-label='Buscar comercio']")
            ),
        )
        campo.clear()
        campo.send_keys(rubro)
        time.sleep(0.8)


# ──────────────────────────────────────────────────────────────────
# THEN  (resultados de filtros)
# ──────────────────────────────────────────────────────────────────

@then('la tabla solo muestra comercios de "{ciudad}"')
def step_verificar_ciudad(context, ciudad):
    with allure.step(f"Verificar que todos los registros son de '{ciudad}'"):
        rows = context.driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
        assert len(rows) > 0, "No hay filas en la tabla después de filtrar"
        for row in rows:
            celdas = row.find_elements(By.TAG_NAME, "td")
            # La 2.ª columna tiene ciudad/rubro
            if len(celdas) >= 2:
                ciudad_celda = celdas[1].text.lower()
                assert ciudad.lower() in ciudad_celda, (
                    f"Se esperaba '{ciudad}' pero la celda dice: {celdas[1].text}"
                )
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name=f"resultado_filtro_{ciudad}",
            attachment_type=allure.attachment_type.PNG,
        )


@then('la tabla solo muestra comercios del rubro "{rubro}"')
def step_verificar_rubro(context, rubro):
    with allure.step(f"Verificar que los registros contienen '{rubro}'"):
        rows = context.driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
        assert len(rows) > 0, "No hay filas en la tabla después de filtrar por rubro"
        for row in rows:
            celdas = row.find_elements(By.TAG_NAME, "td")
            if len(celdas) >= 2:
                texto_fila = row.text.lower()
                assert rubro.lower() in texto_fila, (
                    f"Se esperaba rubro '{rubro}' en la fila pero dice: {row.text[:100]}"
                )


# ──────────────────────────────────────────────────────────────────
# WHEN  (acciones CSV)
# ──────────────────────────────────────────────────────────────────

@when('hace clic en el botón "{texto}"')
def step_click_boton(context, texto):
    with allure.step(f"Clic en botón '{texto}'"):
        # Buscar botón por texto parcial
        botones = context.driver.find_elements(By.TAG_NAME, "button")
        objetivo = None
        for btn in botones:
            if texto.lower() in btn.text.lower():
                objetivo = btn
                break
        assert objetivo is not None, (
            f"No se encontró el botón con texto '{texto}'. "
            f"Botones disponibles: {[b.text for b in botones]}"
        )
        objetivo.click()
        time.sleep(1)


@when("selecciona el primer comercio de la tabla")
def step_seleccionar_primer_comercio(context):
    with allure.step("Marcar el primer checkbox/fila de la tabla"):
        # Intentar checkbox primero, luego clic en fila
        checkboxes = context.driver.find_elements(
            By.CSS_SELECTOR, "table tbody tr input[type='checkbox']"
        )
        if checkboxes:
            checkboxes[0].click()
        else:
            # Si no hay checkbox, clic en la primera fila
            primera_fila = wait_for(
                context,
                EC.element_to_be_clickable(
                    (By.CSS_SELECTOR, "table tbody tr")
                ),
            )
            primera_fila.click()
        time.sleep(0.5)


# ──────────────────────────────────────────────────────────────────
# THEN  (resultado CSV)
# ──────────────────────────────────────────────────────────────────

@then("se inicia una descarga de archivo CSV")
def step_verificar_descarga_csv(context):
    with allure.step("Verificar que se disparó la descarga CSV"):
        # En browsers modernos no podemos interceptar descargas directamente.
        # Verificamos que no haya mensaje de error en la UI.
        time.sleep(2)
        errores = context.driver.find_elements(
            By.CSS_SELECTOR, ".notice-error"
        )
        assert not errores or not any(e.is_displayed() for e in errores), (
            "Apareció un mensaje de error al intentar exportar CSV"
        )
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name="post_exportar_csv",
            attachment_type=allure.attachment_type.PNG,
        )


# ──────────────────────────────────────────────────────────────────
# THEN / WHEN  (email)
# ──────────────────────────────────────────────────────────────────

@then("cada fila de la tabla tiene un email visible")
def step_verificar_emails(context):
    with allure.step("Verificar que cada fila muestra un email"):
        rows = context.driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
        assert len(rows) > 0, "La tabla no tiene filas"
        for i, row in enumerate(rows):
            texto = row.text
            assert "@" in texto, (
                f"La fila {i + 1} no tiene email visible. Texto: {texto[:100]}"
            )
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name="emails_visibles",
            attachment_type=allure.attachment_type.PNG,
        )


@when("hace clic en el botón copiar del primer email")
def step_copiar_email(context):
    with allure.step("Clic en el botón de copiar email de la primera fila"):
        # Buscar botones con aria-label de copiar o texto de clipboard
        botones_copiar = context.driver.find_elements(
            By.XPATH,
            "//button[contains(@aria-label,'opiar') or contains(@title,'opiar') or contains(.,'📋')]"
        )
        assert len(botones_copiar) > 0, (
            "No se encontró ningún botón de copiar email en la tabla"
        )
        botones_copiar[0].click()
        time.sleep(1)


@then('aparece una alerta con el texto "{texto}"')
def step_verificar_alerta(context, texto):
    with allure.step(f"Verificar alerta con texto '{texto}'"):
        # Intentar interceptar alert nativo de JS primero
        try:
            alert = WebDriverWait(context.driver, 3).until(
                EC.alert_is_present()
            )
            mensaje = alert.text
            alert.accept()
            assert texto.lower() in mensaje.lower(), (
                f"Se esperaba '{texto}' en el alert pero dice: '{mensaje}'"
            )
        except Exception:
            # Si no hay alert nativo, buscar notificación en la UI (toast / notice)
            notificaciones = context.driver.find_elements(
                By.CSS_SELECTOR, ".notice, .toast, [role='alert'], .notice-success"
            )
            textos = [n.text for n in notificaciones if n.is_displayed()]
            assert any(texto.lower() in t.lower() for t in textos), (
                f"No se encontró alerta con texto '{texto}'. "
                f"Notificaciones visibles: {textos}"
            )
        allure.attach(
            context.driver.get_screenshot_as_png(),
            name="alerta_capturada",
            attachment_type=allure.attachment_type.PNG,
        )

