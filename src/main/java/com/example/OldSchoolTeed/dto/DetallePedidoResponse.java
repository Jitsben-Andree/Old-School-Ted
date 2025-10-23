package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoResponse {

    // ID correcto (de la tabla 'detalle_pedido')
    private Integer detallePedidoId;

    // Info del producto
    private Integer productoId;
    private String productoNombre;

    // Info de la compra
    private Integer cantidad;
    private BigDecimal precioUnitario; // Precio del producto en ese momento
    private BigDecimal subtotal; // (cantidad * precioUnitario)
    private BigDecimal montoDescuento; // Campo que faltaba
}