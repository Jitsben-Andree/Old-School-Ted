package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromocionResponse {
    private Integer idPromocion;
    private String codigo;
    private String descripcion;
    private BigDecimal descuento;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private boolean activa;
}