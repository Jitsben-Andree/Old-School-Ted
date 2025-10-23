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
public class ProductoRequest {
    private String nombre;
    private String descripcion;
    private String talla; // Ej. "S,M,L,XL" o podríamos normalizarlo
    private BigDecimal precio;
    private Boolean activo;
    private Integer categoriaId;
    // La imagen se manejaría por separado, usualmente con un endpoint de carga de archivos
}
