package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventarioRepository extends JpaRepository<Inventario, Integer> {
    //metodo para buscar el inventario de un producto
    Optional<Inventario> findByProducto(Producto producto);
}
