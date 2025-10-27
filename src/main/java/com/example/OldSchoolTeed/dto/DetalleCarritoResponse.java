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
    private Integer cantidad;
    private BigDecimal precioUnitario; // Precio con descuento si aplica
    private BigDecimal subtotal; // Subtotal con descuento
    private String imageUrl;
    private int stockActual; // <<< NUEVO CAMPO: Stock actual del producto
}

