package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductoService {

    // MÉTODOS DE CONSULTA ---

    List<ProductoResponse> getAllProductosActivos();

    List<ProductoResponse> getAllProductosIncludingInactive();


    ProductoResponse getProductoById(Integer id);


    List<ProductoResponse> getProductosByCategoria(String nombreCategoria);

    // MÉTODOS CRUD (Gestión de Productos) ---
    ProductoResponse createProducto(ProductoRequest request);

    ProductoResponse updateProducto(Integer id, ProductoRequest request);


    void deleteProducto(Integer id);

    // GESTIÓN DE PROMOCIONES ---

    void associatePromocionToProducto(Integer productoId, Integer promocionId);


    //Elimina la asociación de una promoción con un producto.
    void disassociatePromocionFromProducto(Integer productoId, Integer promocionId);

    // --- GESTIÓN DE IMÁGENES Y ARCHIVOS ---
    //Genera un archivo Excel con el listado completo de productos para reportes.

    Resource exportProductosToExcel() throws IOException;


    // Sube o actualiza la imagen de portada (principal) del producto.
    ProductoResponse uploadProductImage(Integer id, MultipartFile file);


    //  Añade una nueva imagen a la galería secundaria del producto.
    ProductoResponse uploadGalleryImage(Integer id, MultipartFile file);


     //Elimina una imagen específica de la galería secundaria
    void deleteGalleryImage(Integer productId, Integer imageId);
}