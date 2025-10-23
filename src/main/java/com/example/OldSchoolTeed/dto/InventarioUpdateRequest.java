package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InventarioUpdateRequest {

    @NotNull(message = "El ID del producto no puede ser nulo")
    private Integer productoId;

    @NotNull(message = "El stock no puede ser nulo")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer nuevoStock;
}
