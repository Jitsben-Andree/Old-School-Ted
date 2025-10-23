package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para mostrar la información de una Categoria
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponse {
    private Integer idCategoria;
    private String nombre;
    private String descripcion;
}