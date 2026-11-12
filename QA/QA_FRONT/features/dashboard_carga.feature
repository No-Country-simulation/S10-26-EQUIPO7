Feature: Carga inicial del Dashboard RepuSense
  Como usuario de RepuSense
  Quiero ver los comercios cargados al abrir el dashboard
  Para poder analizar leads con bajo rating

  Background:
    Given que el usuario abre el dashboard

  Scenario: La tabla muestra comercios al cargar
    Then la tabla debe mostrar al menos un comercio

  Scenario: Las columnas esperadas están presentes
    Then la tabla debe tener la columna "Comercio"
    And la tabla debe tener la columna "Ciudad"
    And la tabla debe tener la columna "Rating"
    And la tabla debe tener la columna "Email"
