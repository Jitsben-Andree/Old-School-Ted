package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pago")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @OneToOne
    @JoinColumn(name = "id_pedido", referencedColumnName = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "fecha_pago")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime fechaPago;

    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado;

    @Column(name = "id_transaccion_externa", length = 255)
    private String idTransaccionExterna;


    public enum MetodoPago {
        YAPE,
        PLIN,
        TARJETA,
        PAYPAL,
        TRANSFERENCIA
    }

    public enum EstadoPago {
        PENDIENTE,
        COMPLETADO,
        FALLIDO
    }

    @PrePersist
    protected void onCreate() {
        this.fechaPago = LocalDateTime.now();
        this.estado = EstadoPago.PENDIENTE;
    }
}
