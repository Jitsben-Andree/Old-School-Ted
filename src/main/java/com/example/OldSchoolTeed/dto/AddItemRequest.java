package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.Min; // Añadir validación
import jakarta.validation.constraints.NotNull; // Añadir validación
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddItemRequest {

    @NotNull(message = "El ID del producto no puede ser nulo")
    private Integer productoId;

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad debe ser al menos 1") // Validar cantidad mínima
    private Integer cantidad;
}
