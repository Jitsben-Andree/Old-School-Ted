package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProductoProveedorRequest;
import com.example.OldSchoolTeed.dto.ProductoProveedorResponse;
import com.example.OldSchoolTeed.dto.UpdatePrecioCostoRequest;

import java.util.List;

public interface ProductoProveedorService {

    /**
     * [Admin] Asigna un producto a un proveedor con un precio de costo.
     */
    ProductoProveedorResponse createAsignacion(ProductoProveedorRequest request);

    /**
     * [Admin] Obtiene todos los proveedores (y sus costos) para un producto específico.
     */
    List<ProductoProveedorResponse> getAsignacionesPorProducto(Integer productoId);

    /**
     * [Admin] Obtiene todos los productos (y sus costos) de un proveedor específico.
     */
    List<ProductoProveedorResponse> getAsignacionesPorProveedor(Integer proveedorId);

    /**
     * [Admin] Actualiza el precio de costo de una asignación existente.
     */
    ProductoProveedorResponse updatePrecioCosto(Integer asignacionId, UpdatePrecioCostoRequest request);

    /**
     * [Admin] Elimina la asignación entre un producto y un proveedor.
     */
    void deleteAsignacion(Integer asignacionId);
}
