package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotBlank(message = "La talla es obligatoria")
    private String talla;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    private Boolean activo;

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoriaId;


    private String colorDorsal;
    private List<LeyendaDto> leyendas;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeyendaDto {
        private String nombre;
        private String numero;
    }
}