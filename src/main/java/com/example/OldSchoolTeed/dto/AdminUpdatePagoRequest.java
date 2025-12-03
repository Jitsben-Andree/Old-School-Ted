package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AdminUpdatePagoRequest {
    @NotEmpty(message = "El nuevo estado de pago no puede estar vacío")
    private String nuevoEstadoPago;
}
