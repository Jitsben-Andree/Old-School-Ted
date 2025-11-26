package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "detalle_carrito")
public class DetalleCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_carrito")
    private Integer idDetalleCarrito;

    @ManyToOne
    @JoinColumn(name = "id_carrito", referencedColumnName = "id_carrito", nullable = false)
    private Carrito carrito;

    @ManyToOne
    @JoinColumn(name = "id_producto", referencedColumnName = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    // --- NUEVOS CAMPOS DE PERSONALIZACIÓN ---

    // Precio base al momento de la compra (para evitar cambios si el admin edita el producto después)
    @Column(name = "precio_base")
    private BigDecimal precioBase;

    @Column(name = "pers_tipo")
    private String personalizacionTipo; // "Leyenda", "Custom"

    @Column(name = "pers_nombre")
    private String personalizacionNombre;

    @Column(name = "pers_numero")
    private String personalizacionNumero;

    @Column(name = "pers_precio")
    private BigDecimal personalizacionPrecio; // Costo extra

    @Column(name = "parche_tipo")
    private String parcheTipo; // "UCL", "LaLiga"

    @Column(name = "parche_precio")
    private BigDecimal parchePrecio; // Costo extra

    // --- MÉTODO AUXILIAR PARA CALCULAR SUBTOTAL ---
    public BigDecimal getSubtotal() {
        BigDecimal totalUnitario = precioBase != null ? precioBase : BigDecimal.ZERO;

        if (personalizacionPrecio != null) {
            totalUnitario = totalUnitario.add(personalizacionPrecio);
        }
        if (parchePrecio != null) {
            totalUnitario = totalUnitario.add(parchePrecio);
        }

        return totalUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}