package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AdminUpdateEnvioRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePagoRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePedidoStatusRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/pedidos") // La ruta base ya está protegida por /admin/** en SecurityConfig
@PreAuthorize("hasAuthority('Administrador')")
public class AdminPedidoController {

    private final PedidoService pedidoService;

    public AdminPedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Endpoint para que el admin vea TODOS los pedidos
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> getAllPedidos() {
        return ResponseEntity.ok(pedidoService.getAllPedidosAdmin());
    }

    // Endpoint para actualizar el ESTADO GENERAL del pedido
    @PatchMapping("/{pedidoId}/estado-pedido")
    public ResponseEntity<PedidoResponse> updatePedidoStatus(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdatePedidoStatusRequest request) {
        PedidoResponse response = pedidoService.updatePedidoStatusAdmin(pedidoId, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint para actualizar el ESTADO DEL PAGO
    @PatchMapping("/{pedidoId}/estado-pago")
    public ResponseEntity<PedidoResponse> updatePagoStatus(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdatePagoRequest request) {
        PedidoResponse response = pedidoService.updatePagoStatusAdmin(pedidoId, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint para actualizar LOS DETALLES DEL ENVÍO
    @PatchMapping("/{pedidoId}/estado-envio")
    public ResponseEntity<PedidoResponse> updateEnvioDetails(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdateEnvioRequest request) {
        PedidoResponse response = pedidoService.updateEnvioDetailsAdmin(pedidoId, request);
        return ResponseEntity.ok(response);
    }
}
