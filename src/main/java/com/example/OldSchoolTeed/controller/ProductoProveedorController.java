package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProductoProveedorRequest;
import com.example.OldSchoolTeed.dto.ProductoProveedorResponse;
import com.example.OldSchoolTeed.dto.UpdatePrecioCostoRequest;
import com.example.OldSchoolTeed.service.ProductoProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asignaciones") // Ruta base (recuerda que el context-path es /api/v1)
@PreAuthorize("hasAuthority('Administrador')") // ¡Toda esta clase es solo para Admins!
public class ProductoProveedorController {

    private final ProductoProveedorService productoProveedorService;

    public ProductoProveedorController(ProductoProveedorService productoProveedorService) {
        this.productoProveedorService = productoProveedorService;
    }

    @PostMapping
    public ResponseEntity<ProductoProveedorResponse> createAsignacion(
            @Valid @RequestBody ProductoProveedorRequest request) {
        ProductoProveedorResponse response = productoProveedorService.createAsignacion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<ProductoProveedorResponse>> getAsignacionesPorProducto(
            @PathVariable Integer productoId) {
        return ResponseEntity.ok(productoProveedorService.getAsignacionesPorProducto(productoId));
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<ProductoProveedorResponse>> getAsignacionesPorProveedor(
            @PathVariable Integer proveedorId) {
        return ResponseEntity.ok(productoProveedorService.getAsignacionesPorProveedor(proveedorId));
    }

    @PutMapping("/{asignacionId}/precio")
    public ResponseEntity<ProductoProveedorResponse> updatePrecioCosto(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody UpdatePrecioCostoRequest request) {
        ProductoProveedorResponse response = productoProveedorService.updatePrecioCosto(asignacionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{asignacionId}")
    public ResponseEntity<Void> deleteAsignacion(@PathVariable Integer asignacionId) {
        productoProveedorService.deleteAsignacion(asignacionId);
        return ResponseEntity.noContent().build();
    }
}