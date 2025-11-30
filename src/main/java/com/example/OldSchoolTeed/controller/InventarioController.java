package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.InventarioResponse;
import com.example.OldSchoolTeed.dto.InventarioUpdateRequest;
import com.example.OldSchoolTeed.service.InventarioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
@PreAuthorize("hasAuthority('Administrador')")
@Slf4j
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PutMapping("/stock")
    public ResponseEntity<InventarioResponse> actualizarStock(@Valid @RequestBody InventarioUpdateRequest request) {
        log.info("Admin: Actualizando stock. ProductoID: {}, Nuevo Stock: {}", request.getProductoId(), request.getNuevoStock());
        InventarioResponse response = inventarioService.actualizarStock(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<InventarioResponse>> obtenerTodoElInventario() {
        log.info("Admin: GET /inventario/all");
        return ResponseEntity.ok(inventarioService.getTodoElInventario());
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<InventarioResponse> obtenerInventarioPorProducto(@PathVariable Integer productoId) {
        log.info("Admin: GET /inventario/producto/{}", productoId);
        return ResponseEntity.ok(inventarioService.getInventarioPorProductoId(productoId));
    }
}