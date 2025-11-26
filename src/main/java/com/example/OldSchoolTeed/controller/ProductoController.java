package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Importante para subir archivos
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200") // Asegúrate de tener esto para que Angular conecte
public class ProductoController {

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // --- ENDPOINTS PÚBLICOS (/productos/**) ---
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponse>> getAllProductosActivos() {
        log.info("GET /productos -> Obteniendo productos activos");
        try {
            return ResponseEntity.ok(productoService.getAllProductosActivos());
        } catch (Exception e) {
            log.error("Error al obtener productos activos", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener productos", e);
        }
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Integer id) {
        log.info("GET /productos/{} -> Obteniendo producto por ID", id);
        try {
            return ResponseEntity.ok(productoService.getProductoById(id));
        } catch (EntityNotFoundException e) {
            log.warn("Producto no encontrado con ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al obtener producto ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener producto", e);
        }
    }

    @GetMapping("/productos/categoria/{nombreCategoria}")
    public ResponseEntity<List<ProductoResponse>> getProductosByCategoria(@PathVariable String nombreCategoria) {
        log.info("GET /productos/categoria/{} -> Obteniendo productos por categoría", nombreCategoria);
        try {
            return ResponseEntity.ok(productoService.getProductosByCategoria(nombreCategoria));
        } catch (Exception e) {
            log.error("Error al obtener productos por categoría '{}'", nombreCategoria, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener productos por categoría", e);
        }
    }

    // --- ENDPOINTS DE ADMINISTRADOR (/admin/productos/**) ---
    @GetMapping("/admin/productos/all")
    public ResponseEntity<List<ProductoResponse>> getAllProductosAdmin() {
        log.info("Admin: GET /admin/productos/all -> Obteniendo todos los productos");
        try {
            return ResponseEntity.ok(productoService.getAllProductosIncludingInactive());
        } catch (Exception e) {
            log.error("Admin: Error al obtener todos los productos", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener todos los productos", e);
        }
    }

    @PostMapping("/admin/productos")
    public ResponseEntity<ProductoResponse> createProductoAdmin(@Valid @RequestBody ProductoRequest request) {
        log.info("Admin: POST /admin/productos -> Creando producto: {}", request.getNombre());
        try {
            ProductoResponse response = productoService.createProducto(request);
            log.info("Admin: Producto creado con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al crear producto, categoría no encontrada: {}", request.getCategoriaId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al crear producto", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear producto", e);
        }
    }

    @PutMapping("/admin/productos/{id}")
    public ResponseEntity<ProductoResponse> updateProductoAdmin(@PathVariable Integer id, @Valid @RequestBody ProductoRequest request) {
        log.info("Admin: PUT /admin/productos/{} -> Actualizando producto", id);
        try {
            ProductoResponse response = productoService.updateProducto(id, request);
            log.info("Admin: Producto ID {} actualizado", id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al actualizar, producto o categoría no encontrado para ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al actualizar producto ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar producto", e);
        }
    }

    @DeleteMapping("/admin/productos/{id}")
    public ResponseEntity<Void> deleteProductoAdmin(@PathVariable Integer id) {
        log.info("Admin: DELETE /admin/productos/{} -> Desactivando producto", id);
        try {
            productoService.deleteProducto(id);
            log.info("Admin: Producto ID {} desactivado (soft delete)", id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al desactivar, producto no encontrado ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al desactivar producto ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al desactivar producto", e);
        }
    }

    // --- GESTIÓN DE PROMOCIONES ---
    @PostMapping("/admin/productos/{productoId}/promociones/{promocionId}")
    public ResponseEntity<Void> associatePromocionAdmin(@PathVariable Integer productoId, @PathVariable Integer promocionId) {
        log.info("Admin: POST /admin/productos/{}/promociones/{} -> Asociando promoción", productoId, promocionId);
        try {
            productoService.associatePromocionToProducto(productoId, promocionId);
            log.info("Admin: Promoción ID {} asociada a Producto ID {}", promocionId, productoId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al asociar promoción, producto o promoción no encontrado.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al asociar promoción", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al asociar promoción", e);
        }
    }

    @DeleteMapping("/admin/productos/{productoId}/promociones/{promocionId}")
    public ResponseEntity<Void> disassociatePromocionAdmin(@PathVariable Integer productoId, @PathVariable Integer promocionId) {
        log.info("Admin: DELETE /admin/productos/{}/promociones/{} -> Desasociando promoción", productoId, promocionId);
        try {
            productoService.disassociatePromocionFromProducto(productoId, promocionId);
            log.info("Admin: Promoción ID {} desasociada de Producto ID {}", promocionId, productoId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al desasociar promoción, producto o promoción no encontrado.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al desasociar promoción", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al desasociar promoción", e);
        }
    }

    //  GESTIÓN DE IMÁGENES ---

    //  Subir/Actualizar Imagen de Portada (Principal)
    @PostMapping("/admin/productos/{id}/imagen")
    public ResponseEntity<ProductoResponse> uploadMainImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {

        log.info("Admin: POST /admin/productos/{}/imagen -> Subiendo portada", id);
        try {
            ProductoResponse response = productoService.uploadProductImage(id, file);
            log.info("Admin: Portada actualizada para producto ID {}", id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al subir portada para producto ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir imagen", e);
        }
    }

    //  Subir Imagen a la Galería
    @PostMapping("/admin/productos/{id}/galeria")
    public ResponseEntity<ProductoResponse> uploadGalleryImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {

        log.info("Admin: POST /admin/productos/{}/galeria -> Agregando imagen a galería", id);
        try {
            ProductoResponse response = productoService.uploadGalleryImage(id, file);
            log.info("Admin: Imagen agregada a galería de producto ID {}", id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al subir imagen a galería para producto ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir a galería", e);
        }
    }

    //  Eliminar Imagen de la Galería
    @DeleteMapping("/admin/productos/{id}/galeria/{imagenId}")
    public ResponseEntity<Void> deleteGalleryImage(
            @PathVariable Integer id,
            @PathVariable Integer imagenId) {

        log.info("Admin: DELETE /admin/productos/{}/galeria/{} -> Borrando imagen de galería", id, imagenId);
        try {
            productoService.deleteGalleryImage(id, imagenId);
            log.info("Admin: Imagen ID {} eliminada de galería producto ID {}", imagenId, id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al borrar imagen de galería", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al borrar imagen", e);
        }
    }

    // --- EXPORTAR EXCEL ---
    @GetMapping("/admin/productos/exportar-excel")
    public ResponseEntity<Resource> exportProductosToExcel() {
        log.info("Admin: GET /admin/productos/exportar-excel -> Solicitud de exportación");
        try {
            Resource file = productoService.exportProductosToExcel();
            String filename = "productos_oldschooltees_"
                    + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);
        } catch (IOException e) {
            log.error("Admin: Error al generar Excel", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el archivo Excel", e);
        }
    }
}