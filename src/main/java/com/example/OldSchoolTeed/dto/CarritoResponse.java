package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoResponse {
    private Integer carritoId;
    private Integer usuarioId;
    private List<DetalleCarritoResponse> items;
    private BigDecimal total;
}
