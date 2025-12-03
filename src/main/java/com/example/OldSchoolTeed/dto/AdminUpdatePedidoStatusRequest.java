package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AdminUpdatePedidoStatusRequest {
    @NotEmpty(message = "El nuevo estado no puede estar vacío")
    private String nuevoEstado;
}
