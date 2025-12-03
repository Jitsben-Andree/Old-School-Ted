package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AdminUpdateEnvioRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePagoRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePedidoStatusRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.service.PedidoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/pedidos")
@PreAuthorize("hasAuthority('Administrador')")
@Slf4j
public class AdminPedidoController {

    private final PedidoService pedidoService;

    public AdminPedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> getAllPedidos() {
        log.info("Admin: Recibida petición GET /admin/pedidos");
        return ResponseEntity.ok(pedidoService.getAllPedidosAdmin());
    }

    @PatchMapping("/{pedidoId}/estado")
    public ResponseEntity<PedidoResponse> updatePedidoStatus(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdatePedidoStatusRequest request
    ) {
        log.info("Admin: Recibida petición PATCH /admin/pedidos/{}/estado con estado: {}", pedidoId, request.getNuevoEstado());
        PedidoResponse pedidoActualizado = pedidoService.updatePedidoStatusAdmin(pedidoId, request);
        log.info("Admin: Estado del pedido ID {} actualizado con éxito.", pedidoId);
        return ResponseEntity.ok(pedidoActualizado);
    }

    @PatchMapping("/{pedidoId}/pago")
    public ResponseEntity<PedidoResponse> updatePagoStatus(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdatePagoRequest request
    ) {
        log.info("Admin: Recibida petición PATCH /admin/pedidos/{}/pago con estado: {}", pedidoId, request.getNuevoEstadoPago());
        PedidoResponse pedidoActualizado = pedidoService.updatePagoStatusAdmin(pedidoId, request);
        log.info("Admin: Estado de pago del pedido ID {} actualizado con éxito.", pedidoId);
        return ResponseEntity.ok(pedidoActualizado);
    }

    @PatchMapping("/{pedidoId}/envio")
    public ResponseEntity<PedidoResponse> updateEnvioDetails(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdateEnvioRequest request
    ) {
        log.info("Admin: Recibida petición PATCH /admin/pedidos/{}/envio con datos: {}", pedidoId, request);
        PedidoResponse pedidoActualizado = pedidoService.updateEnvioDetailsAdmin(pedidoId, request);
        log.info("Admin: Detalles de envío del pedido ID {} actualizados con éxito.", pedidoId);
        return ResponseEntity.ok(pedidoActualizado);
    }

    @DeleteMapping("/{pedidoId}")
    public ResponseEntity<Void> deletePedido(@PathVariable Integer pedidoId) {
        log.info("Admin: Solicitud de eliminación de pedido ID {}", pedidoId);
        pedidoService.deletePedido(pedidoId);
        return ResponseEntity.noContent().build();
    }
}