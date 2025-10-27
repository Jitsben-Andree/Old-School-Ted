package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import org.springframework.core.io.Resource; // << Importar Resource

import java.io.IOException; // << Importar IOException
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
    List<ProductoResponse> getAllProductosIncludingInactive();
    void associatePromocionToProducto(Integer productoId, Integer promocionId);
    void disassociatePromocionFromProducto(Integer productoId, Integer promocionId);

    // --- NUEVO MÉTODO PARA EXPORTAR ---
    /**
     * Genera un archivo Excel (Resource) con la lista de productos.
     * @return Resource que representa el archivo Excel.
     * @throws IOException Si ocurre un error al generar el archivo.
     */
    Resource exportProductosToExcel() throws IOException; // << Nuevo método

}

