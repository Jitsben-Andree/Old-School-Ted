package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    // para buscar x nombre
    List<Producto> findByCategoriaNombre(String nombreCategoria);

    //metodo para buscar los productos activosss
    List<Producto> findByActivoTrue();
}
