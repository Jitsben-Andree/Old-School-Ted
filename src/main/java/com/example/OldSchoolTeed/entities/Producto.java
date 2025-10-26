package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
// Quitar Data si vamos a implementar equals/hashCode manualmente
// import lombok.Data;
import lombok.Getter; // Usar Getter/Setter individualmente
import lombok.NoArgsConstructor;
import lombok.Setter;
// Quitar import innecesario: import org.hibernate.annotations.Fetch;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects; // Importar Objects
import java.util.Set;

@Entity
// @Data // Quitar @Data si implementas equals/hashCode
@Getter // Usar @Getter
@Setter // Usar @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "talla", length = 5)
    @Enumerated(EnumType.STRING)
    private Talla talla;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "producto_promocion",
            joinColumns = @JoinColumn(name = "id_producto"),
            inverseJoinColumns = @JoinColumn(name = "id_promocion")
    )
    private Set<Promocion> promociones = new HashSet<>();


    @Column(name = "image_url", length = 500)
    private String imageUrl;


    public enum Talla {
        S,
        M,
        L,
        XL
    }

    // --- Implementación de equals y hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Usar getClass() para comparar tipos exactos, importante con proxies de Hibernate
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        // Si el ID es null, no son iguales (a menos que sean la misma instancia)
        // Comparar solo por ID si no es null
        return idProducto != null && Objects.equals(idProducto, producto.idProducto);
    }

    @Override
    public int hashCode() {
        // Usar un valor constante si el ID es null, o el hash del ID si no lo es
        // Esto asegura consistencia antes y después de persistir
        return idProducto != null ? Objects.hash(idProducto) : getClass().hashCode();
    }
    // --- Fin equals y hashCode ---
}