
"""
REPUSENSE - DISCOVERY

Este archivo pertenecía al flujo antiguo de descubrimiento
de comercios mediante Google Maps desde Python.

La búsqueda actual se coordina desde Spring Boot,
utilizando SerpApi y PostgreSQL.

Este módulo queda desactivado para evitar:
- Búsquedas duplicadas.
- Inserciones mediante endpoints no confirmados.
- Generación de leads con funciones antiguas.
- Uso accidental de datos ficticios.

No modifica ni elimina registros de PostgreSQL.
"""


def run_real_discovery(rubro="Pizzería", ciudad="La Plata"):
    print("=" * 60)
    print("REPUSENSE - DISCOVERY PYTHON DESACTIVADO")
    print("=" * 60)

    print(f"\nCiudad: {ciudad}")
    print(f"Rubro: {rubro}")

    print("\nLa búsqueda actual debe ejecutarse desde Spring Boot.")
    print("Spring Boot utiliza SerpApi para obtener comercios reales.")

    print("\nNo se realizaron búsquedas.")
    print("No se enviaron solicitudes POST.")
    print("No se modificó PostgreSQL.")
    print("\nPara continuar, utilizá el buscador de RepuSense.")


if __name__ == "__main__":
    import sys

    arg_rubro = sys.argv[1] if len(sys.argv) > 1 else "Pizzería"
    arg_ciudad = sys.argv[2] if len(sys.argv) > 2 else "La Plata"

    run_real_discovery(arg_rubro, arg_ciudad)
