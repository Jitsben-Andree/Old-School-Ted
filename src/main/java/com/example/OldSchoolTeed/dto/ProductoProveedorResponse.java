package com.example.OldSchoolTeed.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductoProveedorResponse {
    private Integer idAsignacion; 
    private Integer productoId;
    private String productoNombre;
    private Integer proveedorId;
    private String proveedorRazonSocial;
    private BigDecimal precioCosto;
}
