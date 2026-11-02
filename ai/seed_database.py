
"""
REPUSENSE - CSV SEEDER

Seeder antiguo utilizado para cargar comercios de prueba
desde resenas_muestra_completa.csv.

Queda desactivado porque RepuSense utiliza actualmente
SerpApi desde Spring Boot para obtener comercios reales.

No se deben insertar datos ficticios en PostgreSQL.
"""


def inferir_rubro(nombre: str) -> str:
    """
    Función auxiliar conservada por compatibilidad.

    No se utiliza para insertar comercios.
    """

    nombre = (nombre or "").lower()

    if any(k in nombre for k in ["dental", "clínica", "clinica", "odontol"]):
        return "Salud / Clínica Dental"

    if any(k in nombre for k in ["restaurante", "pizza", "burger", "café", "cafe", "hamburgues"]):
        return "Gastronomía / Cafetería"

    if any(k in nombre for k in ["taller", "mecánic", "mecanic", "pistón", "auto"]):
        return "Servicios / Mecánica"

    if any(k in nombre for k in ["estética", "estetica", "spa", "belleza", "peluquer"]):
        return "Salud / Estética"

    if any(k in nombre for k in ["veterinaria", "huellitas", "mascota", "animal"]):
        return "Salud / Veterinaria"

    return "Comercio Minorista / Retail"


def run_seeder():
    """
    Desactiva la carga de datos desde el CSV de muestra.
    No realiza inserciones ni modifica la base de datos.
    """

    print("=" * 60)
    print("REPUSENSE - CSV SEEDER DESACTIVADO")
    print("=" * 60)

    print("\nNo se cargarán comercios desde archivos CSV.")
    print("No se generarán identificadores ficticios.")
    print("No se inventarán teléfonos, sitios web ni calificaciones.")

    print("\nLos comercios reales se obtienen mediante SerpApi")
    print("desde el backend Spring Boot.")

    print("\nNo se modificó PostgreSQL.")


if __name__ == "__main__":
    run_seeder()
