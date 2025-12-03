package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.entities.ProductoProveedor;
import com.example.OldSchoolTeed.entities.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoProveedorRepository extends JpaRepository<ProductoProveedor, Integer> {


    boolean existsByProveedor(Proveedor proveedor);


    List<ProductoProveedor> findByProducto(Producto producto);


    List<ProductoProveedor> findByProveedor(Proveedor proveedor);


    Optional<ProductoProveedor> findByProductoAndProveedor(Producto producto, Proveedor proveedor);
}