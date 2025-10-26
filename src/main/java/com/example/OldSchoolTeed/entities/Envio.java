package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "envio")
public class Envio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio")
    private Integer idEnvio;

    @OneToOne
    @JoinColumn(name = "id_pedido", referencedColumnName = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "fecha_envio")
    @Temporal(TemporalType.DATE)
    private LocalDate fechaEnvio;

    @Column(name = "direccion_envio", length = 300, nullable = false)
    private String direccionEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEnvio estado;

    @Column(name = "codigo_seguimiento", length = 100)
    private String codigoSeguimiento;

    public enum EstadoEnvio {
        EN_PREPARACION,
        EN_CAMINO,
        ENTREGADO,
        RETRASADO
    }

    @PrePersist
    protected void onCreate() {
        this.estado = EstadoEnvio.EN_PREPARACION;
    }
}
