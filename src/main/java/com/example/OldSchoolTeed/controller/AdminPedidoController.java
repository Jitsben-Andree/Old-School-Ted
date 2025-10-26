package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AdminUpdateEnvioRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePagoRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePedidoStatusRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.service.PedidoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Para manejo de errores

import java.util.List;

@RestController
@RequestMapping("/admin/pedidos") // Ruta base para admin
@PreAuthorize("hasAuthority('Administrador')") // ¡Toda la clase requiere rol Admin!
public class AdminPedidoController {

    private static final Logger log = LoggerFactory.getLogger(AdminPedidoController.class); // Añadir logger
    private final PedidoService pedidoService;

    public AdminPedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * [Admin] Obtiene todos los pedidos de todos los usuarios.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('Administrador')") // Reforzar por si acaso
    public ResponseEntity<List<PedidoResponse>> getAllPedidos() {
        log.info("Admin: Recibida petición GET /admin/pedidos"); // Log de entrada
        try {
            List<PedidoResponse> pedidos = pedidoService.getAllPedidosAdmin();
            return ResponseEntity.ok(pedidos);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al obtener todos los pedidos", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener pedidos", e);
        }
    }

    /**
     * [Admin] Actualiza el estado general de un pedido (PENDIENTE, PAGADO, ENVIADO, etc.).
     */
    @PatchMapping("/{pedidoId}/estado")
    @PreAuthorize("hasAuthority('Administrador')") // Añadir explícitamente aquí
    public ResponseEntity<PedidoResponse> updatePedidoStatus(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdatePedidoStatusRequest request
    ) {
        // Log para verificar si el método es alcanzado
        log.info("Admin: Recibida petición PATCH /admin/pedidos/{}/estado con estado: {}", pedidoId, request.getNuevoEstado());
        try {
            PedidoResponse pedidoActualizado = pedidoService.updatePedidoStatusAdmin(pedidoId, request);
            log.info("Admin: Estado del pedido ID {} actualizado con éxito.", pedidoId);
            return ResponseEntity.ok(pedidoActualizado);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Intento de actualizar estado de pedido no encontrado ID: {}", pedidoId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Admin: Estado de pedido inválido recibido para ID {}: {}", pedidoId, request.getNuevoEstado());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al actualizar estado del pedido ID {}", pedidoId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar estado del pedido", e);
        }
    }

    /**
     * [Admin] Actualiza el estado del pago de un pedido (PENDIENTE, COMPLETADO, FALLIDO).
     */
    @PatchMapping("/{pedidoId}/pago")
    @PreAuthorize("hasAuthority('Administrador')") // Añadir explícitamente aquí
    public ResponseEntity<PedidoResponse> updatePagoStatus(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdatePagoRequest request
    ) {
        // Log para verificar si el método es alcanzado
        log.info("Admin: Recibida petición PATCH /admin/pedidos/{}/pago con estado: {}", pedidoId, request.getNuevoEstadoPago());
        try {
            PedidoResponse pedidoActualizado = pedidoService.updatePagoStatusAdmin(pedidoId, request);
            log.info("Admin: Estado de pago del pedido ID {} actualizado con éxito.", pedidoId);
            return ResponseEntity.ok(pedidoActualizado);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Intento de actualizar pago de pedido no encontrado ID: {}", pedidoId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Admin: Estado de pago inválido recibido para ID {}: {}", pedidoId, request.getNuevoEstadoPago());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al actualizar estado de pago del pedido ID {}", pedidoId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar estado del pago", e);
        }
    }

    /**
     * [Admin] Actualiza los detalles del envío de un pedido (dirección, fecha, estado).
     */
    @PatchMapping("/{pedidoId}/envio")
    @PreAuthorize("hasAuthority('Administrador')") // Añadir explícitamente aquí
    public ResponseEntity<PedidoResponse> updateEnvioDetails(
            @PathVariable Integer pedidoId,
            @Valid @RequestBody AdminUpdateEnvioRequest request // Asegúrate que este DTO no tenga validaciones conflictivas
    ) {
        // Log para verificar si el método es alcanzado
        log.info("Admin: Recibida petición PATCH /admin/pedidos/{}/envio con datos: {}", pedidoId, request); // Loguea el request completo
        try {
            PedidoResponse pedidoActualizado = pedidoService.updateEnvioDetailsAdmin(pedidoId, request);
            log.info("Admin: Detalles de envío del pedido ID {} actualizados con éxito.", pedidoId);
            return ResponseEntity.ok(pedidoActualizado);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Intento de actualizar envío de pedido no encontrado ID: {}", pedidoId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Admin: Estado de envío inválido recibido para ID {}: {}", pedidoId, request.getNuevoEstadoEnvio());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al actualizar detalles de envío del pedido ID {}", pedidoId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar detalles del envío", e);
        }
    }
}

