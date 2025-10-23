package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;

import java.util.List;

public interface ProductoService {

    // --- Endpoints Públicos ---
    List<ProductoResponse> getAllProductosActivos();
    ProductoResponse getProductoById(Integer id);
    List<ProductoResponse> getProductosByCategoria(String nombreCategoria);

    // --- Endpoints de Administrador ---
    ProductoResponse createProducto(ProductoRequest request);
    ProductoResponse updateProducto(Integer id, ProductoRequest request);
    void deleteProducto(Integer id); // Soft delete (cambiar activo a false)
}
