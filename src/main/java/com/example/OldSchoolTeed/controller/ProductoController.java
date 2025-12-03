package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }


    @GetMapping("/error-unico")
    public void errorUnico() {
        // UUID genera un código único tipo: "a1b2-c3d4-..."
        String codigoUnico = java.util.UUID.randomUUID().toString();
        throw new RuntimeException("Error ÚNICO de prueba ID: " + codigoUnico);
    }

    //  ENDPOINTS PÚBLICOS (/productos/)
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponse>> getAllProductosActivos() {
        log.info("GET /productos -> Obteniendo productos activos");
        return ResponseEntity.ok(productoService.getAllProductosActivos());
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Integer id) {
        log.info("GET /productos/{} -> Obteniendo producto por ID", id);
        return ResponseEntity.ok(productoService.getProductoById(id));
    }

    @GetMapping("/productos/categoria/{nombreCategoria}")
    public ResponseEntity<List<ProductoResponse>> getProductosByCategoria(@PathVariable String nombreCategoria) {
        log.info("GET /productos/categoria/{} -> Obteniendo productos por categoría", nombreCategoria);
        return ResponseEntity.ok(productoService.getProductosByCategoria(nombreCategoria));
    }

    //  ENDPOINTS DE ADMINISTRADOR (/admin/productos/**)
    @GetMapping("/admin/productos/all")
    public ResponseEntity<List<ProductoResponse>> getAllProductosAdmin() {
        log.info("Admin: GET /admin/productos/all -> Obteniendo todos los productos");
        return ResponseEntity.ok(productoService.getAllProductosIncludingInactive());
    }

    @PostMapping("/admin/productos")
    public ResponseEntity<ProductoResponse> createProductoAdmin(@Valid @RequestBody ProductoRequest request) {
        log.info("Admin: POST /admin/productos -> Creando producto: {}", request.getNombre());
        ProductoResponse response = productoService.createProducto(request);
        log.info("Admin: Producto creado con ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/productos/{id}")
    public ResponseEntity<ProductoResponse> updateProductoAdmin(@PathVariable Integer id, @Valid @RequestBody ProductoRequest request) {
        log.info("Admin: PUT /admin/productos/{} -> Actualizando producto", id);
        ProductoResponse response = productoService.updateProducto(id, request);
        log.info("Admin: Producto ID {} actualizado", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/productos/{id}")
    public ResponseEntity<Void> deleteProductoAdmin(@PathVariable Integer id) {
        log.info("Admin: DELETE /admin/productos/{} -> Desactivando producto", id);
        productoService.deleteProducto(id);
        log.info("Admin: Producto ID {} desactivado (soft delete)", id);
        return ResponseEntity.noContent().build();
    }

    //  GESTIÓN DE PROMOCIONES
    @PostMapping("/admin/productos/{productoId}/promociones/{promocionId}")
    public ResponseEntity<Void> associatePromocionAdmin(@PathVariable Integer productoId, @PathVariable Integer promocionId) {
        log.info("Admin: POST /admin/productos/{}/promociones/{} -> Asociando promoción", productoId, promocionId);
        productoService.associatePromocionToProducto(productoId, promocionId);
        log.info("Admin: Promoción ID {} asociada a Producto ID {}", promocionId, productoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/productos/{productoId}/promociones/{promocionId}")
    public ResponseEntity<Void> disassociatePromocionAdmin(@PathVariable Integer productoId, @PathVariable Integer promocionId) {
        log.info("Admin: DELETE /admin/productos/{}/promociones/{} -> Desasociando promoción", productoId, promocionId);
        productoService.disassociatePromocionFromProducto(productoId, promocionId);
        log.info("Admin: Promoción ID {} desasociada de Producto ID {}", promocionId, productoId);
        return ResponseEntity.noContent().build();
    }

    //  GESTIÓN DE IMÁGENES

    @PostMapping("/admin/productos/{id}/imagen")
    public ResponseEntity<ProductoResponse> uploadMainImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Admin: POST /admin/productos/{}/imagen -> Subiendo portada", id);
        ProductoResponse response = productoService.uploadProductImage(id, file);
        log.info("Admin: Portada actualizada para producto ID {}", id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/productos/{id}/galeria")
    public ResponseEntity<ProductoResponse> uploadGalleryImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Admin: POST /admin/productos/{}/galeria -> Agregando imagen a galería", id);
        ProductoResponse response = productoService.uploadGalleryImage(id, file);
        log.info("Admin: Imagen agregada a galería de producto ID {}", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/productos/{id}/galeria/{imagenId}")
    public ResponseEntity<Void> deleteGalleryImage(
            @PathVariable Integer id,
            @PathVariable Integer imagenId) {

        log.info("Admin: DELETE /admin/productos/{}/galeria/{} -> Borrando imagen de galería", id, imagenId);
        productoService.deleteGalleryImage(id, imagenId);
        log.info("Admin: Imagen ID {} eliminada de galería producto ID {}", imagenId, id);
        return ResponseEntity.noContent().build();
    }

    //  EXPORTAR EXCEL
    @GetMapping("/admin/productos/exportar-excel")
    public ResponseEntity<Resource> exportProductosToExcel() throws IOException {
        log.info("Admin: GET /admin/productos/exportar-excel -> Solicitud de exportación");

        Resource file = productoService.exportProductosToExcel();
        String filename = "productos_oldschooltees_"
                + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}