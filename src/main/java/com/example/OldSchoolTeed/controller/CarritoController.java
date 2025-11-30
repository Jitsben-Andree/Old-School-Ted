package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.UpdateCantidadRequest;
import com.example.OldSchoolTeed.service.CarritoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/carrito")
@PreAuthorize("hasAnyAuthority('Cliente', 'Administrador')")
@Slf4j
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    private String getEmailFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            log.error("Error Auth: Principal no válido.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado correctamente.");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @GetMapping("/mi-carrito")
    public ResponseEntity<CarritoResponse> getMiCarrito(Authentication authentication) {
        String userEmail = getEmailFromAuthentication(authentication);
        log.info("GET /carrito/mi-carrito solicitado por: {}", userEmail);
        return ResponseEntity.ok(carritoService.getCarritoByUsuario(userEmail));
    }

    @PostMapping("/agregar")
    public ResponseEntity<CarritoResponse> addItem(
            @Valid @RequestBody AddItemRequest request,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        log.info("POST /carrito/agregar -> User: {}, ProdID: {}, Cant: {}", email, request.getProductoId(), request.getCantidad());
        CarritoResponse carrito = carritoService.addItemToCarrito(email, request);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping("/eliminar/{detalleCarritoId}")
    public ResponseEntity<CarritoResponse> removeItem(
            @PathVariable Integer detalleCarritoId,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        log.info("DELETE /carrito/eliminar/{} solicitado por: {}", detalleCarritoId, email);
        CarritoResponse carrito = carritoService.removeItemFromCarrito(email, detalleCarritoId);
        return ResponseEntity.ok(carrito);
    }

    @PutMapping("/actualizar-cantidad/{detalleCarritoId}")
    public ResponseEntity<CarritoResponse> updateQuantity(
            @PathVariable Integer detalleCarritoId,
            @Valid @RequestBody UpdateCantidadRequest request,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        log.info("PUT /actualizar-cantidad/{} -> User: {}, NuevaCant: {}", detalleCarritoId, email, request.getNuevaCantidad());
        CarritoResponse carrito = carritoService.updateItemQuantity(email, detalleCarritoId, request);
        return ResponseEntity.ok(carrito);
    }
}