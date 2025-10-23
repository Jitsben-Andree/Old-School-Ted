package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.ProveedorRequest;
import com.example.OldSchoolTeed.dto.ProveedorResponse;
import com.example.OldSchoolTeed.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proveedores") // Ruta base (recuerda que el context-path es /api/v1)
@PreAuthorize("hasAuthority('Administrador')") // ¡Toda esta clase es solo para Admins!
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> createProveedor(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse response = proveedorService.createProveedor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> getAllProveedores() {
        return ResponseEntity.ok(proveedorService.getAllProveedores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> getProveedorById(@PathVariable Integer id) {
        return ResponseEntity.ok(proveedorService.getProveedorById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> updateProveedor(@PathVariable Integer id, @Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse response = proveedorService.updateProveedor(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProveedor(@PathVariable Integer id) {
        proveedorService.deleteProveedor(id);
        return ResponseEntity.noContent().build();
    }
}