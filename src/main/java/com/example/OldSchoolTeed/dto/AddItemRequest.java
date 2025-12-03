package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddItemRequest {

    @NotNull(message = "El ID del producto no puede ser nulo")
    private Integer productoId;

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;


    private PersonalizacionRequest personalizacion;
    private ParcheRequest parche;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalizacionRequest {
        private String tipo;
        private String nombre;
        private String numero;
        private BigDecimal precio;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParcheRequest {
        private String tipo;
        private BigDecimal precio;
    }
}