package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para crear un nuevo pedido
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequest {
    // La dirección de envío, que irá a la entidad Envio
    private String direccionEnvio;

    // Método de pago (simplificado por ahora)
    // En un futuro, esto podría ser un ID de tarjeta o un token de pago
    private String metodoPagoInfo;
}
