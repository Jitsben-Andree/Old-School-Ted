package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; // Importar Valid
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/productos") // Ruta base
public class ProductoController {

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class); // Añadir logger
    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // --- ENDPOINTS PÚBLICOS (definidos como GET en SecurityConfig) ---

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getAllProductosActivos() {
        log.info("GET /productos -> Obteniendo productos activos");
        try {
            return ResponseEntity.ok(productoService.getAllProductosActivos());
        } catch (Exception e) {
            log.error("Error al obtener productos activos", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener productos", e);
        }
    }

    @GetMapping("/{id}")
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

    @GetMapping("/categoria/{nombreCategoria}")
    public ResponseEntity<List<ProductoResponse>> getProductosByCategoria(@PathVariable String nombreCategoria) {
        log.info("GET /productos/categoria/{} -> Obteniendo productos por categoría", nombreCategoria);
        try {
            return ResponseEntity.ok(productoService.getProductosByCategoria(nombreCategoria));
        } catch (Exception e) {
            log.error("Error al obtener productos por categoría '{}'", nombreCategoria, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener productos por categoría", e);
        }
    }

    // --- ENDPOINTS DE ADMINISTRADOR (Protegidos) ---

    // Nuevo endpoint para que el Admin vea TODOS los productos
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<List<ProductoResponse>> getAllProductosAdmin() {
        log.info("Admin: GET /productos/admin/all -> Obteniendo todos los productos");
        try {
            return ResponseEntity.ok(productoService.getAllProductosIncludingInactive());
        } catch (Exception e) {
            log.error("Admin: Error al obtener todos los productos", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener todos los productos", e);
        }
    }


    @PostMapping
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<ProductoResponse> createProducto(@Valid @RequestBody ProductoRequest request) { // Añadir @Valid
        log.info("Admin: POST /productos -> Creando producto: {}", request.getNombre());
        try {
            ProductoResponse response = productoService.createProducto(request);
            log.info("Admin: Producto creado con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al crear producto, categoría no encontrada: {}", request.getCategoriaId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); // 400 si la categoría no existe
        } catch (Exception e) {
            log.error("Admin: Error inesperado al crear producto", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear producto", e);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<ProductoResponse> updateProducto(@PathVariable Integer id, @Valid @RequestBody ProductoRequest request) { // Añadir @Valid
        log.info("Admin: PUT /productos/{} -> Actualizando producto", id);
        try {
            ProductoResponse response = productoService.updateProducto(id, request);
            log.info("Admin: Producto ID {} actualizado", id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al actualizar, producto o categoría no encontrado para ID: {}", id);
            // Puede ser producto no encontrado o categoría no encontrada
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al actualizar producto ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar producto", e);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> deleteProducto(@PathVariable Integer id) {
        log.info("Admin: DELETE /productos/{} -> Desactivando producto", id);
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

    // --- NUEVOS ENDPOINTS PARA ASOCIAR PROMOCIONES ---

    @PostMapping("/{productoId}/promociones/{promocionId}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> associatePromocion(
            @PathVariable Integer productoId,
            @PathVariable Integer promocionId
    ) {
        log.info("Admin: POST /productos/{}/promociones/{} -> Asociando promoción", productoId, promocionId);
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

    @DeleteMapping("/{productoId}/promociones/{promocionId}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> disassociatePromocion(
            @PathVariable Integer productoId,
            @PathVariable Integer promocionId
    ) {
        log.info("Admin: DELETE /productos/{}/promociones/{} -> Desasociando promoción", productoId, promocionId);
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
}
