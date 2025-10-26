package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO simple para evitar referencias circulares al enviar promociones con productos
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromocionSimpleDto {
    private Integer idPromocion;
    private String codigo;
    private String descripcion;
    private BigDecimal descuento;
    private boolean activa;
}
