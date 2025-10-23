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

    /**
     * Verifica si existe alguna relación para un proveedor específico.
     * Es usado para prevenir el borrado de un proveedor que esté asociado a productos.
     */
    boolean existsByProveedor(Proveedor proveedor);

    /**
     * Busca todas las asignaciones para un producto específico.
     * (Ej: ¿Quién me provee esta camiseta?)
     */
    List<ProductoProveedor> findByProducto(Producto producto);

    /**
     * Busca todas las asignaciones de un proveedor específico.
     * (Ej: ¿Qué camisetas me provee este proveedor?)
     */
    List<ProductoProveedor> findByProveedor(Proveedor proveedor);

    /**
     * Verifica si ya existe una combinación de producto y proveedor.
     * (Usado para prevenir duplicados al crear una nueva asignación)
     */
    Optional<ProductoProveedor> findByProductoAndProveedor(Producto producto, Proveedor proveedor);
}