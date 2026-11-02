
"""
REPUSENSE - GOOGLE MAPS SERVICE

Compatibilidad con scripts antiguos.

La búsqueda actual de comercios se realiza desde Spring Boot
mediante SerpApi.

Este módulo ya no consulta Google Places API directamente.

Se conserva search_businesses_in_maps() para evitar errores
de importación en scripts antiguos mientras se migra el proyecto.

No realiza solicitudes externas ni modifica PostgreSQL.
"""


def search_businesses_in_maps(rubro="Pizzería", ciudad="La Plata"):
    """
    Función obsoleta.

    La búsqueda real debe ejecutarse desde el backend Spring Boot,
    que utiliza SerpApi y guarda los resultados en PostgreSQL.
    """

    print("=" * 60)
    print("REPUSENSE - BÚSQUEDA DE COMERCIOS")
    print("=" * 60)

    print(f"Rubro: {rubro}")
    print(f"Ciudad: {ciudad}")

    print(
        "\nEsta función de Python está desactivada."
        "\nLa búsqueda actual se realiza desde Spring Boot mediante SerpApi."
    )

    return []


if __name__ == "__main__":
    search_businesses_in_maps("Farmacia", "La Plata")
