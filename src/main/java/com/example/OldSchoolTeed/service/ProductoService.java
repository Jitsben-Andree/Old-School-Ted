package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductoService {

    // --- MÉTODOS DE CONSULTA ---

    /**
     * Obtiene solo los productos que tienen el flag 'activo' en true.
     * Ideal para la vista pública de la tienda.
     */
    List<ProductoResponse> getAllProductosActivos();

    /**
     * Obtiene TODOS los productos (activos e inactivos).
     * Ideal para el panel de administración.
     */
    List<ProductoResponse> getAllProductosIncludingInactive();

    /**
     * Busca un producto por su ID. Lanza excepción si no existe.
     */
    ProductoResponse getProductoById(Integer id);

    /**
     * Filtra productos activos por nombre de categoría.
     */
    List<ProductoResponse> getProductosByCategoria(String nombreCategoria);

    // --- MÉTODOS CRUD (Gestión de Productos) ---

    /**
     * Crea un nuevo producto con sus datos básicos, precio, talla y categoría.
     * También maneja la creación inicial de inventario y las nuevas opciones de personalización (color/leyendas).
     */
    ProductoResponse createProducto(ProductoRequest request);

    /**
     * Actualiza los datos de un producto existente.
     */
    ProductoResponse updateProducto(Integer id, ProductoRequest request);

    /**
     * Desactiva un producto (Soft Delete) cambiando su estado 'activo' a false.
     */
    void deleteProducto(Integer id);

    // --- GESTIÓN DE PROMOCIONES ---

    /**
     * Asocia una promoción existente a un producto.
     */
    void associatePromocionToProducto(Integer productoId, Integer promocionId);

    /**
     * Elimina la asociación de una promoción con un producto.
     */
    void disassociatePromocionFromProducto(Integer productoId, Integer promocionId);

    // --- GESTIÓN DE IMÁGENES Y ARCHIVOS ---

    /**
     * Genera un archivo Excel con el listado completo de productos para reportes.
     */
    Resource exportProductosToExcel() throws IOException;

    /**
     * Sube o actualiza la imagen de portada (principal) del producto.
     */
    ProductoResponse uploadProductImage(Integer id, MultipartFile file);

    /**
     * Añade una nueva imagen a la galería secundaria del producto.
     */
    ProductoResponse uploadGalleryImage(Integer id, MultipartFile file);

    /**
     * Elimina una imagen específica de la galería secundaria.
     */
    void deleteGalleryImage(Integer productId, Integer imageId);
}