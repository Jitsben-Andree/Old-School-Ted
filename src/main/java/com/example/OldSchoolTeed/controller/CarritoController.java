package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.service.CarritoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carrito")
// ¡¡AQUÍ ESTÁ EL CAMBIO!!
// Cambiamos de "hasAuthority('Cliente')" a "hasAnyAuthority" para permitir ambos roles
@PreAuthorize("hasAnyAuthority('Cliente', 'Administrador')")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /**
     * Obtiene el email del usuario autenticado desde el token JWT.
     */
    private String getEmailFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @GetMapping("/mi-carrito")
    public ResponseEntity<CarritoResponse> getMiCarrito(Authentication authentication) {
        String email = getEmailFromAuthentication(authentication);
        return ResponseEntity.ok(carritoService.getCarritoByUsuario(email));
    }

    @PostMapping("/agregar")
    public ResponseEntity<CarritoResponse> addItem(
            @RequestBody AddItemRequest request,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        try {
            CarritoResponse carrito = carritoService.addItemToCarrito(email, request);
            return ResponseEntity.ok(carrito);
        } catch (RuntimeException e) {
            // Captura errores de stock o producto no encontrado
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/eliminar/{detalleCarritoId}")
    public ResponseEntity<CarritoResponse> removeItem(
            @PathVariable Integer detalleCarritoId,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        try {
            CarritoResponse carrito = carritoService.removeItemFromCarrito(email, detalleCarritoId);
            return ResponseEntity.ok(carrito);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
