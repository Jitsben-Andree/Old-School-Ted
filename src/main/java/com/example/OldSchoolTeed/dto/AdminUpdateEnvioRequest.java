package com.example.OldSchoolTeed.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminUpdateEnvioRequest {

    private String nuevoEstadoEnvio; // Ej: "EN_PREPARACION", "EN_CAMINO", "ENTREGADO"

    private String direccionEnvio; // Para confirmar o corregir la dirección

    private LocalDate fechaEnvio; // Para registrar cuándo se envió
}