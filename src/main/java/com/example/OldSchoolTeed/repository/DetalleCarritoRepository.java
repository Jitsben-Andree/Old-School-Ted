package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Carrito;
import com.example.OldSchoolTeed.entities.DetalleCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCarritoRepository extends JpaRepository<DetalleCarrito, Integer> {

    /**
     * Busca todos los items (detalles) asociados a un carrito específico.
     * Spring Data JPA entiende este nombre de método y genera la consulta SQL automáticamente.
     *
     * @param carrito El carrito del cual se quieren obtener los detalles.
     * @return Una lista de DetalleCarrito.
     */
    List<DetalleCarrito> findByCarrito(Carrito carrito);
}
