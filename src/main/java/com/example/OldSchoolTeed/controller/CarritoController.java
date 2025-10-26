package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.service.CarritoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/carrito")
// ¡ASEGÚRATE DE QUE ESTA LÍNEA ESTÉ ASÍ! Permite ambos roles.
@PreAuthorize("hasAnyAuthority('Cliente', 'Administrador')")
public class CarritoController {

    private static final Logger log = LoggerFactory.getLogger(CarritoController.class);
    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    private String getEmailFromAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            log.error("No se pudo obtener UserDetails de la autenticación.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado correctamente");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        log.debug("Obteniendo email del usuario autenticado: {}", userDetails.getUsername());
        return userDetails.getUsername();
    }

    @GetMapping("/mi-carrito")
    public ResponseEntity<CarritoResponse> getMiCarrito(Authentication authentication) {
        String email = getEmailFromAuthentication(authentication);
        log.info("Recibida petición GET /mi-carrito para usuario: {}", email);
        try {
            CarritoResponse carrito = carritoService.getCarritoByUsuario(email);
            log.info("Carrito obtenido con éxito para usuario: {}", email);
            return ResponseEntity.ok(carrito);
        } catch (EntityNotFoundException e) {
            log.warn("No se encontró el usuario al obtener el carrito: {}", email, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al obtener el carrito para usuario: {}", email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener el carrito", e);
        }
    }

    @PostMapping("/agregar")
    public ResponseEntity<CarritoResponse> addItem(
            @Valid @RequestBody AddItemRequest request,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        log.info("Recibida petición POST /agregar para usuario: {}, Producto ID: {}, Cantidad: {}",
                email, request.getProductoId(), request.getCantidad());
        try {
            CarritoResponse carrito = carritoService.addItemToCarrito(email, request);
            log.info("Item añadido/actualizado con éxito al carrito para usuario: {}", email);
            return ResponseEntity.ok(carrito);
        } catch (EntityNotFoundException e) {
            log.warn("Error al añadir item (Not Found): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (RuntimeException e) {
            log.warn("Error al añadir item (Bad Request): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al añadir item al carrito para usuario: {}", email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al añadir item al carrito", e);
        }
    }

    @DeleteMapping("/eliminar/{detalleCarritoId}")
    public ResponseEntity<CarritoResponse> removeItem(
            @PathVariable Integer detalleCarritoId,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        log.info("Recibida petición DELETE /eliminar/{} para usuario: {}", detalleCarritoId, email);
        try {
            CarritoResponse carrito = carritoService.removeItemFromCarrito(email, detalleCarritoId);
            log.info("Item {} eliminado con éxito del carrito para usuario: {}", detalleCarritoId, email);
            return ResponseEntity.ok(carrito);
        } catch (SecurityException e) {
            log.warn("Intento no autorizado de eliminar item {} por usuario {}", detalleCarritoId, email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (EntityNotFoundException e) {
            log.warn("Error al eliminar item (Not Found): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al eliminar item {} del carrito para usuario: {}", detalleCarritoId, email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar item del carrito", e);
        }
    }
}

