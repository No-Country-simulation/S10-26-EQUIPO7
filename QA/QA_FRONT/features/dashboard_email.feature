Feature: Email y acciones del Dashboard RepuSense
  Como usuario de RepuSense
  Quiero poder copiar el email de un comercio
  Para contactar leads rápidamente

  Background:
    Given que el usuario abre el dashboard
    And espera que la tabla cargue datos

  Scenario: Columna Email muestra datos en cada fila
    Then cada fila de la tabla tiene un email visible

  Scenario: Botón copiar email muestra alerta de confirmación
    When hace clic en el botón copiar del primer email
    Then aparece una alerta con el texto "Email copiado"
