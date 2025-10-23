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
    private BigDecimal precio;
    private Boolean activo;
    private String categoriaNombre; // Enviamos el nombre de la categoría, no el ID
    private int stock; // Añadimos el stock desde la entidad Inventario
    // private String imageUrl; // (Cuando implementemos imágenes)
}
