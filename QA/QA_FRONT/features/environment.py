"""
environment.py — Configuración global de Behave para QA_FRONT
- Levanta Chrome automáticamente (sin instalar ChromeDriver manualmente)
- Graba video de pantalla durante cada escenario
- Realiza el inicio de sesión previo a cada escenario
- Adjunta capturas y video al reporte Allure en caso de fallo
"""
from asyncio import wait
import os
import time
import threading
import allure
import mss
import imageio
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager

BASE_URL = "http://localhost:5173"
VIDEO_DIR = os.path.join(os.path.dirname(__file__), "..", "videos")
os.makedirs(VIDEO_DIR, exist_ok=True)


# ──────────────────────────────────────────────
# Clase auxiliar para grabación de pantalla
# ──────────────────────────────────────────────
class ScreenRecorder:
    """Graba la pantalla en un hilo de fondo y guarda un MP4."""

    def __init__(self, filepath: str, fps: int = 10):
        self.filepath = filepath
        self.fps = fps
        self._frames: list = []
        self._running = False
        self._thread = threading.Thread(target=self._capture_loop, daemon=True)

    def start(self):
        self._running = True
        self._thread.start()

    def stop(self):
        self._running = False
        self._thread.join(timeout=5)
        if self._frames:
            imageio.mimwrite(self.filepath, self._frames, fps=self.fps, codec="libx264")

    def _capture_loop(self):
        with mss.mss() as sct:
            monitor = sct.monitors[0]  # Pantalla completa
            while self._running:
                img = sct.grab(monitor)
                frame = imageio.core.util.Array(img)
                # mss devuelve BGRA → convertir a RGB
                self._frames.append(frame[:, :, :3])
                time.sleep(1 / self.fps)


# ──────────────────────────────────────────────
# Hooks de Behave
# ──────────────────────────────────────────────
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
import time
import os
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager

def before_all(context):
    """Se ejecuta una sola vez antes de todos los escenarios."""
    context.base_url = "http://localhost:5173" # O tu variable BASE_URL

def before_scenario(context, scenario):
    options = webdriver.ChromeOptions()
    user_data_path = os.path.expanduser("~") + "/selenium_chrome_profile"
    options.add_argument(f"--user-data-dir={user_data_path}")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--disable-notifications")
    options.add_experimental_option("excludeSwitches", ["enable-logging"])

    service = Service(ChromeDriverManager().install())
    context.driver = webdriver.Chrome(service=service, options=options)
    context.driver.implicitly_wait(10)
    context.driver.maximize_window()

    # 1. Ir directamente a la URL principal de la app
    context.driver.get(context.base_url)
    
    # Damos un respiro corto para que cargue la SPA
    time.sleep(2)

    # 2. VERIFICACIÓN INTELIGENTE: ¿Ya estamos logueados por el perfil persistente?
    # Buscamos si existe el input de email (lo que significaría que estamos en la pantalla de login)
    inputs_login = context.driver.find_elements(By.CSS_SELECTOR, "input[type='email']")

    if len(inputs_login) > 0:
        print(">> No hay sesión activa. Realizando login por única vez...")
        
        # Rellenar email correcto
        email_input = inputs_login[0]
        email_input.clear()
        email_input.send_keys("teregcorigliano@gmail.com")
        
        # Rellenar contraseña
        password_input = context.driver.find_element(By.CSS_SELECTOR, "input[type='password']")
        password_input.clear()
        password_input.send_keys("12345678")
        
        time.sleep(1)
        # Enviar con ENTER
        password_input.send_keys(Keys.RETURN)
        
        # Esperar a que procese y entre al dashboard
        time.sleep(3)
    else:
        print(">> Sesión recuperada exitosamente desde el perfil persistente. Saltando login.")

   
    # Iniciar grabación
    video_name = scenario.name.replace(" ", "_").replace("/", "-")[:60]
    video_path = os.path.join(VIDEO_DIR, f"{video_name}.mp4")
    context.recorder = ScreenRecorder(video_path)
    context.recorder.start()
    context._video_path = video_path


def after_scenario(context, scenario):
    """
    Después de cada escenario:
    1. Detiene grabación y guarda el MP4
    2. Si falló: adjunta screenshot + video al reporte Allure
    3. Cierra el navegador
    """
    # Detener grabación
    if hasattr(context, "recorder"):
        context.recorder.stop()

    # Si el escenario falló → adjuntar evidencia
    if scenario.status == "failed":
        # Screenshot del momento del fallo
        try:
            screenshot = context.driver.get_screenshot_as_png()
            allure.attach(
                screenshot,
                name="screenshot_fallo",
                attachment_type=allure.attachment_type.PNG,
            )
        except Exception:
            pass

        # Video del escenario
        try:
            if os.path.exists(context._video_path):
                with open(context._video_path, "rb") as f:
                    allure.attach(
                        f.read(),
                        name="video_fallo",
                        attachment_type=allure.attachment_type.MP4,
                    )
        except Exception:
            pass

    # Cerrar navegador
    if hasattr(context, "driver"):
        context.driver.quit()

