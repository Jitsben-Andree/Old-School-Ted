package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProveedorRequest;
import com.example.OldSchoolTeed.dto.ProveedorResponse;
import com.example.OldSchoolTeed.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proveedores")
@PreAuthorize("hasAuthority('Administrador')")
@Slf4j
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> createProveedor(@Valid @RequestBody ProveedorRequest request) {
        log.info("Admin: Creando proveedor: {}", request.getRazonSocial());
        ProveedorResponse response = proveedorService.createProveedor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> getAllProveedores() {
        log.info("Admin: Listando proveedores");
        return ResponseEntity.ok(proveedorService.getAllProveedores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> getProveedorById(@PathVariable Integer id) {
        log.info("Admin: Consultando proveedor ID {}", id);
        return ResponseEntity.ok(proveedorService.getProveedorById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> updateProveedor(@PathVariable Integer id, @Valid @RequestBody ProveedorRequest request) {
        log.info("Admin: Actualizando proveedor ID {}", id);
        ProveedorResponse response = proveedorService.updateProveedor(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProveedor(@PathVariable Integer id) {
        log.info("Admin: Eliminando proveedor ID {}", id);
        proveedorService.deleteProveedor(id);
        return ResponseEntity.noContent().build();
    }
}