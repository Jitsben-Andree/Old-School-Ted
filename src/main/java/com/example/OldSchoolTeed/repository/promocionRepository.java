package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface promocionRepository extends JpaRepository<Promocion, Integer> {

    //este metodo para buscar un codigo de promocion
    Optional<Promocion> findByCodigo(String codigo);

    //este metodo lo utilizare para buscar promociones activas
    // now == fecha inicio   now2== fecha
    List<Promocion> findByActivaTrueAndFechaInicioBeforeAndFechaFinAfter(LocalDateTime now, LocalDateTime now2);
}
