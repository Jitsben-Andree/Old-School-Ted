package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePrecioCostoRequest {

    @NotNull(message = "El nuevo precio de costo es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de costo debe ser mayor que 0")
    private BigDecimal nuevoPrecioCosto;
}
