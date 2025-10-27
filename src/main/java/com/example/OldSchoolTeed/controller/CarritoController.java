package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.UpdateCantidadRequest;
import com.example.OldSchoolTeed.service.CarritoService;
// Quitar import innecesario si no se usa directamente en catch:
// import io.jsonwebtoken.security.SecurityException;
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
@RequestMapping("/carrito") // Ruta base
// Permitir Cliente o Admin
@PreAuthorize("hasAnyAuthority('Cliente', 'Administrador')")
public class CarritoController {

    private static final Logger log = LoggerFactory.getLogger(CarritoController.class);
    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /**
     * Obtiene el email del usuario autenticado desde el token JWT.
     */
    private String getEmailFromAuthentication(Authentication authentication) {
        // Validación robusta de Authentication y Principal
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            log.error("Error al obtener email: Authentication inválida o principal no es UserDetails. Authentication: {}", authentication);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado correctamente.");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // Validar que el username (email) no sea null o vacío
        if (userDetails.getUsername() == null || userDetails.getUsername().trim().isEmpty()){
            log.error("Error al obtener email: UserDetails tiene username vacío o null.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Información de usuario inválida en token.");
        }
        return userDetails.getUsername();
    }

    @GetMapping("/mi-carrito")
    public ResponseEntity<CarritoResponse> getMiCarrito(Authentication authentication) {
        log.info("GET /carrito/mi-carrito solicitado.");
        String userEmail = "desconocido"; // Inicializar email
        try {
            userEmail = getEmailFromAuthentication(authentication);
            log.debug("Obteniendo carrito para usuario: {}", userEmail);
            CarritoResponse carrito = carritoService.getCarritoByUsuario(userEmail);
            return ResponseEntity.ok(carrito);
        } catch (EntityNotFoundException e) {
            log.warn("Usuario {} intentó obtener carrito pero no se encontró.", userEmail, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (ResponseStatusException e) { // Capturar errores específicos de estado (ej. 401 de getEmail)
            log.warn("ResponseStatusException al obtener carrito para {}: Status={}, Reason={}", userEmail, e.getStatusCode(), e.getReason());
            throw e; // Re-lanzar
        } catch (Exception e) {
            log.error("Error inesperado al obtener 'mi-carrito' para usuario {}", userEmail, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al obtener el carrito", e);
        }
    }

    @PostMapping("/agregar")
    public ResponseEntity<CarritoResponse> addItem(
            @Valid @RequestBody AddItemRequest request,
            Authentication authentication
    ) {
        if (request == null || request.getProductoId() == null || request.getCantidad() == null) {
            log.error("Error en POST /carrito/agregar: Request body o sus campos son null.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de solicitud inválidos.");
        }

        log.info("POST /carrito/agregar solicitado: Producto ID {}, Cantidad {}", request.getProductoId(), request.getCantidad());
        String email = "desconocido";

        try {
            email = getEmailFromAuthentication(authentication);
            CarritoResponse carrito = carritoService.addItemToCarrito(email, request);
            log.info("Item añadido/actualizado con éxito para usuario {}", email);
            return ResponseEntity.ok(carrito);

        } catch (EntityNotFoundException e) {
            log.warn("Error al agregar item para usuario {}: {}", email, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);

        } catch (RuntimeException e) { // Captura errores de negocio (Stock, Cantidad inválida)
            log.warn("Error de lógica de negocio al agregar item para usuario {}: {}", email, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) { // Captura otros errores (incluyendo ResponseStatusException)
            // Captura tanto ResponseStatusException como otros errores inesperados
            if (e instanceof ResponseStatusException rse) { // Usar pattern variable binding (Java 16+)
                log.warn("ResponseStatusException al agregar item para usuario {}: Status={}, Reason={}", email, rse.getStatusCode(), rse.getReason());
                throw rse; // Re-lanzar ResponseStatusException
            }
            // Si no es ResponseStatusException, tratar como error inesperado
            log.error("Error inesperado al agregar item para usuario {}", email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al agregar item al carrito", e);
        }
    }


    @DeleteMapping("/eliminar/{detalleCarritoId}")
    public ResponseEntity<CarritoResponse> removeItem(
            @PathVariable Integer detalleCarritoId,
            Authentication authentication
    ) {
        log.info("DELETE /carrito/eliminar/{} solicitado", detalleCarritoId);
        if (detalleCarritoId == null || detalleCarritoId <= 0) {
            log.error("Error en DELETE /carrito/eliminar: ID de detalle inválido: {}", detalleCarritoId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de detalle de carrito inválido.");
        }
        String email = "desconocido"; // Inicializar email
        try {
            email = getEmailFromAuthentication(authentication); // Obtener email primero
            CarritoResponse carrito = carritoService.removeItemFromCarrito(email, detalleCarritoId);
            log.info("Item {} eliminado con éxito para usuario {}", detalleCarritoId, email);
            return ResponseEntity.ok(carrito);
        } catch (EntityNotFoundException e) {
            log.warn("Error al eliminar item {} para usuario {}: {}", detalleCarritoId, email, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            // Usar java.lang.SecurityException
        } catch (java.lang.SecurityException e) { // Captura si el item no pertenece al usuario
            log.error("Intento no autorizado de eliminar item {} por usuario {}", detalleCarritoId, email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (ResponseStatusException e) { // Captura otros errores de estado (ej. 401)
            log.warn("ResponseStatusException al eliminar item {} para usuario {}: Status={}, Reason={}", detalleCarritoId, email, e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al eliminar item {} para usuario {}", detalleCarritoId, email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al eliminar item del carrito", e);
        }
    }

    // --- NUEVO ENDPOINT: updateItemQuantity ---
    @PutMapping("/actualizar-cantidad/{detalleCarritoId}")
    public ResponseEntity<CarritoResponse> updateQuantity(
            @PathVariable Integer detalleCarritoId,
            @Valid @RequestBody UpdateCantidadRequest request,
            Authentication authentication
    ) {
        if (detalleCarritoId == null || detalleCarritoId <= 0) {
            log.error("Error en PUT /actualizar-cantidad: ID de detalle inválido: {}", detalleCarritoId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de detalle de carrito inválido.");
        }
        if (request == null || request.getNuevaCantidad() == null) {
            log.error("Error en PUT /actualizar-cantidad/{}: Request body o nuevaCantidad es null.", detalleCarritoId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de solicitud inválidos.");
        }

        log.info("PUT /carrito/actualizar-cantidad/{} solicitado con nueva cantidad: {}", detalleCarritoId, request.getNuevaCantidad());
        String email = "desconocido";

        try {
            email = getEmailFromAuthentication(authentication);
            CarritoResponse carrito = carritoService.updateItemQuantity(email, detalleCarritoId, request);
            log.info("Cantidad actualizada con éxito para item {} del usuario {}", detalleCarritoId, email);
            return ResponseEntity.ok(carrito);

        } catch (EntityNotFoundException e) {
            log.warn("Error al actualizar cantidad para item {} de usuario {}: {}", detalleCarritoId, email, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);

        } catch (RuntimeException e) { // Captura errores de negocio (Stock, Cantidad inválida)
            log.warn("Error de lógica de negocio al actualizar cantidad para item {} de usuario {}: {}", detalleCarritoId, email, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) { // Captura SecurityException, ResponseStatusException y otros
            // Manejo unificado para SecurityException y ResponseStatusException
            // Usar java.lang.SecurityException
            if (e instanceof java.lang.SecurityException se) {
                log.error("Intento no autorizado de actualizar item {} por usuario {}", detalleCarritoId, email);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, se.getMessage(), se);
            }
            if (e instanceof ResponseStatusException rse) {
                log.warn("ResponseStatusException al actualizar cantidad para item {} de usuario {}: Status={}, Reason={}", detalleCarritoId, email, rse.getStatusCode(), rse.getReason());
                throw rse; // Re-lanzar
            }
            // Si no es ninguna de las anteriores, es inesperado
            log.error("Error inesperado al actualizar cantidad del item {} para usuario {}", detalleCarritoId, email, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al actualizar cantidad del item", e);
        }
    }
    // --- FIN NUEVO ENDPOINT ---
}

