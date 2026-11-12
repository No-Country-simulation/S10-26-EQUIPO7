Feature: Filtros del Dashboard RepuSense
  Como usuario de RepuSense
  Quiero filtrar los comercios por ciudad y rubro
  Para encontrar leads específicos

  Background:
    Given que el usuario abre el dashboard
    And espera que la tabla cargue datos

  Scenario: Filtrar por ciudad Córdoba
    When selecciona la ciudad "cordoba"
    Then la tabla solo muestra comercios de "Córdoba"

  Scenario: Filtrar por rubro Restaurante
    When selecciona el rubro "Restaurante"
    Then la tabla solo muestra comercios del rubro "Restaurante"

  Scenario: Limpiar filtro de ciudad restaura todos los registros
    When selecciona la ciudad "cordoba"
    And limpia el filtro de ciudad
    Then la tabla debe mostrar al menos un comercio
