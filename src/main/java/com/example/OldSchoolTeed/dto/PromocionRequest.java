package com.example.OldSchoolTeed.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromocionRequest {

    @NotEmpty(message = "El código de la promoción no puede estar vacío")
    private String codigo;

    @NotEmpty(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "El descuento no puede ser nulo")
    @Positive(message = "El descuento debe ser un valor positivo")
    private BigDecimal descuento;

    @NotNull(message = "La fecha de inicio no puede ser nula")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin no puede ser nula")
    @Future(message = "La fecha de fin debe ser en el futuro")
    private LocalDateTime fechaFin;

    private boolean activa = true;
}
