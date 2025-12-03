package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importar Query
import org.springframework.data.repository.query.Param; // Importar Param

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Integer> {

    // metodo para buscar un codigo de promocion
    Optional<Promocion> findByCodigo(String codigo);

    // metodo lo utilizare para buscar promociones activas
    // now == fecha inicio   now2== fecha
    List<Promocion> findByActivaTrueAndFechaInicioBeforeAndFechaFinAfter(LocalDateTime now, LocalDateTime now2);


    @Query("SELECT p FROM Promocion p JOIN p.productos prod WHERE prod.idProducto = :productoId AND p.activa = true AND :now BETWEEN p.fechaInicio AND p.fechaFin")
    List<Promocion> findActivePromocionesForProducto(@Param("productoId") Integer productoId, @Param("now") LocalDateTime now);
}
