package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProductoProveedorRequest;
import com.example.OldSchoolTeed.dto.ProductoProveedorResponse;
import com.example.OldSchoolTeed.dto.UpdatePrecioCostoRequest;
import com.example.OldSchoolTeed.service.ProductoProveedorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/asignaciones")
@PreAuthorize("hasAuthority('Administrador')")
@Slf4j
public class ProductoProveedorController {

    private final ProductoProveedorService productoProveedorService;

    public ProductoProveedorController(ProductoProveedorService productoProveedorService) {
        this.productoProveedorService = productoProveedorService;
    }

    @PostMapping
    public ResponseEntity<ProductoProveedorResponse> createAsignacion(
            @Valid @RequestBody ProductoProveedorRequest request) {
        log.info("Admin: Asignando proveedor ID {} a producto ID {}", request.getProveedorId(), request.getProductoId());
        ProductoProveedorResponse response = productoProveedorService.createAsignacion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<ProductoProveedorResponse>> getAsignacionesPorProducto(
            @PathVariable Integer productoId) {
        log.info("Admin: Consultando proveedores del producto ID {}", productoId);
        return ResponseEntity.ok(productoProveedorService.getAsignacionesPorProducto(productoId));
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<ProductoProveedorResponse>> getAsignacionesPorProveedor(
            @PathVariable Integer proveedorId) {
        log.info("Admin: Consultando productos del proveedor ID {}", proveedorId);
        return ResponseEntity.ok(productoProveedorService.getAsignacionesPorProveedor(proveedorId));
    }

    @PutMapping("/{asignacionId}/precio")
    public ResponseEntity<ProductoProveedorResponse> updatePrecioCosto(
            @PathVariable Integer asignacionId,
            @Valid @RequestBody UpdatePrecioCostoRequest request) {
        log.info("Admin: Actualizando costo asignación ID {}: {}", asignacionId, request.getNuevoPrecioCosto());
        ProductoProveedorResponse response = productoProveedorService.updatePrecioCosto(asignacionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{asignacionId}")
    public ResponseEntity<Void> deleteAsignacion(@PathVariable Integer asignacionId) {
        log.info("Admin: Eliminando asignación ID {}", asignacionId);
        productoProveedorService.deleteAsignacion(asignacionId);
        return ResponseEntity.noContent().build();
    }
}