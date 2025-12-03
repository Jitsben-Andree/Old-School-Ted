package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Carrito;
import com.example.OldSchoolTeed.entities.DetalleCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCarritoRepository extends JpaRepository<DetalleCarrito, Integer> {


    List<DetalleCarrito> findByCarrito(Carrito carrito);
}
