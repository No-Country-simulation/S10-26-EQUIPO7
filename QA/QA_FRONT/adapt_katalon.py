"""
adapt_katalon.py
────────────────────────────────────────────────────────────────────
Script de ayuda para adaptar scripts exportados por Katalon Studio
(formato Python WebDriverUnittest) al estilo Behave usado en QA_FRONT.

USO:
    python adapt_katalon.py mi_script_katalon.py

Qué hace:
1. Lee el script exportado por Katalon
2. Muestra las acciones detectadas (find_element, click, send_keys…)
3. Genera una plantilla de step functions lista para pegar en dashboard_steps.py
────────────────────────────────────────────────────────────────────
"""
import re
import sys


KATALON_PATTERNS = [
    (r"self\.driver\.find_element\((.+?)\)\.click\(\)", "click"),
    (r"self\.driver\.find_element\((.+?)\)\.send_keys\((.+?)\)", "send_keys"),
    (r"self\.driver\.find_element\((.+?)\)\.clear\(\)", "clear"),
    (r"self\.driver\.get\(['\"](.+?)['\"]\)", "get"),
    (r"self\.driver\.find_element\((.+?)\)", "find"),
]


def parse_katalon(filepath: str):
    with open(filepath, encoding="utf-8") as f:
        content = f.read()

    actions = []
    for pattern, action in KATALON_PATTERNS:
        for match in re.finditer(pattern, content):
            groups = match.groups()
            actions.append((action, groups, match.start()))

    # Ordenar por posición en el archivo
    actions.sort(key=lambda x: x[2])
    return actions, content


def generate_step_template(actions):
    lines = [
        "# ── PLANTILLA GENERADA AUTOMÁTICAMENTE ──",
        "# Revisá cada step y ajustá los selectores si es necesario.",
        "",
        "from behave import when, then",
        "from selenium.webdriver.common.by import By",
        "import time",
        "",
        "",
        '@when("ejecuto el flujo exportado de Katalon")',
        "def step_katalon_flow(context):",
    ]

    for i, (action, groups, _) in enumerate(actions):
        comment = f"    # Acción {i + 1}: {action}"
        lines.append(comment)

        if action == "get":
            lines.append(f"    context.driver.get({groups[0]!r})")

        elif action == "click":
            selector = groups[0]
            lines.append(f"    context.driver.find_element({selector}).click()")

        elif action == "send_keys":
            selector, value = groups
            lines.append(
                f"    context.driver.find_element({selector}).send_keys({value})"
            )

        elif action == "clear":
            selector = groups[0]
            lines.append(f"    context.driver.find_element({selector}).clear()")

        lines.append("    time.sleep(0.3)")
        lines.append("")

    return "\n".join(lines)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python adapt_katalon.py <archivo_exportado_katalon.py>")
        sys.exit(1)

    filepath = sys.argv[1]
    print(f"\nAnalizando: {filepath}\n")

    actions, _ = parse_katalon(filepath)
    print(f"Se detectaron {len(actions)} acciones:\n")
    for i, (action, groups, _) in enumerate(actions):
        print(f"  {i + 1}. [{action}] {groups}")

    template = generate_step_template(actions)
    output_file = filepath.replace(".py", "_behave_steps.py")

    with open(output_file, "w", encoding="utf-8") as f:
        f.write(template)

    print(f"\n✅ Plantilla generada en: {output_file}")
    print("Pegá el contenido en features/steps/dashboard_steps.py y ajustá los steps.")

