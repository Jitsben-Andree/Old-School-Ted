package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCarritoResponse {
    private Integer detalleCarritoId;
    private Integer productoId;
    private String productoNombre;
    private String imageUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario; // Precio base
    private BigDecimal subtotal;       // Precio total de la línea (con extras)
    private Integer stockActual;

    // --- NUEVOS CAMPOS PARA VISUALIZAR ---
    private String personalizacionTipo;   // "Leyenda" o "Custom"
    private String personalizacionNombre; // Ej: "MESSI"
    private String personalizacionNumero; // Ej: "10"
    private BigDecimal personalizacionPrecio;

    private String parcheTipo;            // Ej: "UCL"
    private BigDecimal parchePrecio;
}