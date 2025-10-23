package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.PedidoRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos") // Ruta base (recuerda que el context-path es /api/v1)
@PreAuthorize("hasAuthority('Cliente')") // Requiere rol 'Cliente' para toda la clase
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    private String getEmailFromAuthentication(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @PostMapping("/crear")
    public ResponseEntity<PedidoResponse> crearPedido(
            @RequestBody PedidoRequest request,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        try {
            PedidoResponse pedido = pedidoService.crearPedidoDesdeCarrito(email, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
        } catch (RuntimeException e) {
            // Captura errores de stock, carrito vacío, etc.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponse>> getMisPedidos(Authentication authentication) {
        String email = getEmailFromAuthentication(authentication);
        return ResponseEntity.ok(pedidoService.getPedidosByUsuario(email));
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<PedidoResponse> getPedidoPorId(
            @PathVariable Integer pedidoId,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        try {
            PedidoResponse pedido = pedidoService.getPedidoById(email, pedidoId);
            return ResponseEntity.ok(pedido);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}