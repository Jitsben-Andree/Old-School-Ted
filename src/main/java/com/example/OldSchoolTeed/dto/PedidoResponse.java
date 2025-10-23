package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponse {
    private Integer pedidoId;
    private LocalDateTime fecha;
    private String estado; // Pendiente, Pagado, Enviado, etc.
    private BigDecimal total;
    private List<DetallePedidoResponse> detalles;

    // Info del envío
    private String direccionEnvio;
    private String estadoEnvio;

    // Info del pago
    private String estadoPago;
    private String metodoPago;
}
