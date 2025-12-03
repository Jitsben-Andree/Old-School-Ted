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
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Integer stockActual;


    private String personalizacionTipo;
    private String personalizacionNombre;
    private String personalizacionNumero;
    private BigDecimal personalizacionPrecio;

    private String parcheTipo;
    private BigDecimal parchePrecio;
}