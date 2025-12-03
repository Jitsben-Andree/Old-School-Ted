package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
// import lombok.Data; // Quitar si implementas equals/hashCode
import lombok.Getter; // Usar Getter
import lombok.NoArgsConstructor;
import lombok.Setter; // Usar Setter

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects; // Importar Objects
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promocion")
public class Promocion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocion")
    private Integer idPromocion;

    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2, nullable = false)
    private BigDecimal descuento;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;


    @ManyToMany(mappedBy = "promociones", fetch = FetchType.LAZY)
    private Set<Producto> productos = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Promocion promocion = (Promocion) o;
        return idPromocion != null && Objects.equals(idPromocion, promocion.idPromocion);
    }

    @Override
    public int hashCode() {
        return idPromocion != null ? Objects.hash(idPromocion) : getClass().hashCode();
    }
}
