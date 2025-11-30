package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.PedidoRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.service.PedidoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@PreAuthorize("hasAuthority('Cliente')")
@Slf4j
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
        log.info("Cliente: Creando pedido para usuario: {}", email);
        // Si falla por stock o carrito vacío, lanzará RuntimeException que atrapará el GlobalHandler
        PedidoResponse pedido = pedidoService.crearPedidoDesdeCarrito(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponse>> getMisPedidos(Authentication authentication) {
        String email = getEmailFromAuthentication(authentication);
        log.info("Cliente: Consultando historial pedidos: {}", email);
        return ResponseEntity.ok(pedidoService.getPedidosByUsuario(email));
    }

    @GetMapping("/{pedidoId}")
    public ResponseEntity<PedidoResponse> getPedidoPorId(
            @PathVariable Integer pedidoId,
            Authentication authentication
    ) {
        String email = getEmailFromAuthentication(authentication);
        log.info("Cliente: Consultando pedido ID {} para usuario {}", pedidoId, email);
        return ResponseEntity.ok(pedidoService.getPedidoById(email, pedidoId));
    }
}