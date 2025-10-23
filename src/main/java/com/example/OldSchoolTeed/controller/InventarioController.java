package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.InventarioResponse;
import com.example.OldSchoolTeed.dto.InventarioUpdateRequest;
import com.example.OldSchoolTeed.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario") // Ruta base (recuerda que el context-path es /api/v1)
@PreAuthorize("hasAuthority('Administrador')") // ¡Toda esta clase es solo para Admins!
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PutMapping("/stock")
    public ResponseEntity<InventarioResponse> actualizarStock(@Valid @RequestBody InventarioUpdateRequest request) {
        InventarioResponse response = inventarioService.actualizarStock(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<InventarioResponse>> obtenerTodoElInventario() {
        return ResponseEntity.ok(inventarioService.getTodoElInventario());
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<InventarioResponse> obtenerInventarioPorProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(inventarioService.getInventarioPorProductoId(productoId));
    }
}
