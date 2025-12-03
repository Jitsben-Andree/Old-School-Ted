package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventario")
public class Inventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Integer idInventario;

    @OneToOne
    @JoinColumn(name = "id_producto", referencedColumnName = "id_producto", nullable = false, unique = true)
    private Producto producto;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "ultima_actualizacion")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime ultimaActualizacion;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        this.ultimaActualizacion = LocalDateTime.now();
    }

    public Inventario(Producto producto, int stock) {
        this.producto = producto;
        this.stock = stock;
        this.setUltimaActualizacion(java.time.LocalDateTime.now());
    }
}
