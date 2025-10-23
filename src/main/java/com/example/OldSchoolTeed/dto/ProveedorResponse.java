package com.example.OldSchoolTeed.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProveedorResponse {
    private Integer idProveedor;
    private String razonSocial;
    private String contacto;
    private String telefono;
    private String direccion;
}
