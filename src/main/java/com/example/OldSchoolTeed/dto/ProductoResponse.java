package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String talla;
    private BigDecimal precio; // Este será el precio CON DESCUENTO si aplica
    private Boolean activo;
    private String categoriaNombre;
    private int stock;
    private String imageUrl; // Mantenemos el campo de imagen

    // --- Nuevos Campos para Promociones ---
    private BigDecimal precioOriginal; // El precio base sin descuento
    private BigDecimal descuentoAplicado; // El porcentaje o monto del descuento aplicado (opcional)
    private String nombrePromocion; // Nombre de la promoción aplicada (opcional)

}

