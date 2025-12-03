package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequest {

    private String direccionEnvio;


    private String metodoPagoInfo;
}
