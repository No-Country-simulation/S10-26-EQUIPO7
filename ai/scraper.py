
"""
REPUSENSE - PROCESS EXTRACTED PLACES

Módulo antiguo de procesamiento y persistencia de comercios.

La búsqueda actual se realiza desde Spring Boot mediante SerpApi.
Spring Boot también se encarga de guardar los comercios en PostgreSQL.

Este módulo queda desactivado para evitar:
- Inserciones duplicadas.
- Uso de endpoints no confirmados.
- Datos de comercios simulados.
- Valores ficticios para teléfonos y sitios web.

No realiza solicitudes HTTP ni modifica PostgreSQL.
"""


def process_extracted_places(places):
    """
    Función conservada por compatibilidad con módulos antiguos.

    No procesa ni guarda comercios.
    La búsqueda y persistencia corresponden al backend.
    """

    total = len(places) if places else 0

    print("=" * 60)
    print("REPUSENSE - PROCESAMIENTO DE COMERCIOS")
    print("=" * 60)

    print(f"\nComercios recibidos: {total}")

    print(
        "\nEste procesamiento de Python está desactivado."
        "\nLa búsqueda y persistencia se realizan desde Spring Boot."
    )

    print("\nNo se enviaron solicitudes POST.")
    print("No se modificó PostgreSQL.")

    return 0


def send_business_to_backend(business_data):
    """
    Función antigua conservada por compatibilidad.

    No envía comercios al backend.
    """

    print(
        "Operación desactivada: "
        "la persistencia de comercios corresponde a Spring Boot."
    )

    return None


if __name__ == "__main__":
    print("Este módulo no ejecuta búsquedas ni inserciones.")
    print("Utilizá el buscador de RepuSense para obtener comercios reales.")
