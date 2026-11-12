@echo off
REM ============================================================
REM  run_tests.bat — Ejecuta las pruebas de front-end de RepuSense
REM  Uso: Doble clic o ejecutar desde cmd en la carpeta QA_FRONT
REM ============================================================

echo.
echo ============================================================
echo   RepuSense QA_FRONT — Ejecutando suite de pruebas
echo ============================================================
echo.

REM Ir al directorio del script
cd /d "%~dp0"

REM Limpiar resultados anteriores
if exist allure-results rmdir /s /q allure-results
mkdir allure-results

if exist videos rmdir /s /q videos
mkdir videos

echo [1/3] Ejecutando escenarios Gherkin con Behave...
behave --no-capture ^
  -f allure_behave.formatter:AllureFormatter ^
  -o allure-results ^
  -f pretty ^
  features/

echo.
echo [2/3] Generando reporte Allure...
allure generate allure-results -o allure-report --clean

echo.
echo [3/3] Abriendo reporte en el navegador...
allure open allure-report

pause

