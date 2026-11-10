package com.repusense.backend.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeleteBusinessesRequest(
        @NotEmpty(message = "Seleccioná al menos un comercio.")
        @Size(max = 100, message = "No se pueden eliminar más de 100 comercios por operación.")
        List<@NotNull @Positive Long> ids) {
}
