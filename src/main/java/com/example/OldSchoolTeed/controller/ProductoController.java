package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productos") // Ruta base (recuerda que el context-path es /api/v1)
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // --- ENDPOINTS PÚBLICOS (definidos como GET en SecurityConfig) ---

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getAllProductosActivos() {
        return ResponseEntity.ok(productoService.getAllProductosActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(productoService.getProductoById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/categoria/{nombreCategoria}")
    public ResponseEntity<List<ProductoResponse>> getProductosByCategoria(@PathVariable String nombreCategoria) {
        return ResponseEntity.ok(productoService.getProductosByCategoria(nombreCategoria));
    }

    // --- ENDPOINTS DE ADMINISTRADOR (Protegidos) ---

    @PostMapping
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<ProductoResponse> createProducto(@RequestBody ProductoRequest request) {
        try {
            ProductoResponse response = productoService.createProducto(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<ProductoResponse> updateProducto(@PathVariable Integer id, @RequestBody ProductoRequest request) {
        try {
            return ResponseEntity.ok(productoService.updateProducto(id, request));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> deleteProducto(@PathVariable Integer id) {
        try {
            productoService.deleteProducto(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
