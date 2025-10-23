package com.example.OldSchoolTeed.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventarioResponse {
    private Integer inventarioId;
    private Integer productoId;
    private String productoNombre;
    private Integer stock;
    private LocalDateTime ultimaActualizacion;
}
