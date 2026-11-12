Feature: Exportación CSV del Dashboard RepuSense
  Como usuario de RepuSense
  Quiero exportar los datos a CSV
  Para poder trabajar con ellos offline

  Background:
    Given que el usuario abre el dashboard
    And espera que la tabla cargue datos

  Scenario: Exportar todos los comercios a CSV
    When hace clic en el botón "Exportar Todos"
    Then se inicia una descarga de archivo CSV

  Scenario: Exportar comercios seleccionados a CSV
    When selecciona el primer comercio de la tabla
    And hace clic en el botón "Exportar Seleccionados"
    Then se inicia una descarga de archivo CSV
