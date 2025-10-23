package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear o actualizar una Categoria
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaRequest {
    @NotEmpty(message = "El nombre de la categoría no puede estar vacío")
    private String nombre;

    private String descripcion;
}
