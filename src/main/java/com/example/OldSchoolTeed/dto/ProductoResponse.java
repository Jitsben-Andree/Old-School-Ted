package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List; // Importar List
import java.util.Set; // Importar Set (si usas Set en la entidad)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String talla;
    private BigDecimal precio; // Precio final (con descuento si aplica)
    private Boolean activo;
    private String categoriaNombre;
    private int stock;
    private String imageUrl;

    // --- Campos de Promoción ---
    private BigDecimal precioOriginal;
    private BigDecimal descuentoAplicado;
    private String nombrePromocion;

    // --- NUEVO CAMPO ---
    private List<PromocionSimpleDto> promocionesAsociadas; // Lista de promociones de este producto
}

