package com.example.OldSchoolTeed.dto;

import lombok.Data;
import jakarta.validation.constraints.Size; // Importar Size
import java.time.LocalDate;

@Data // Lombok genera getters y setters
public class AdminUpdateEnvioRequest {


    private String nuevoEstadoEnvio; // Ej: "EN_PREPARACION", "EN_CAMINO", "ENTREGADO"


    @Size(max = 300, message = "La dirección no puede exceder los 300 caracteres")
    private String direccionEnvio;


    private LocalDate fechaEnvio;


    @Size(max = 100, message = "El código de seguimiento no puede exceder los 100 caracteres")
    private String codigoSeguimiento; // Campo que faltaba
}
