package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.PedidoRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;

import java.util.List;

public interface PedidoService {

    /**
     * Crea un nuevo pedido a partir del carrito del usuario.
     * Valida stock, reduce inventario y vacía el carrito.
     * @param userEmail Email del usuario autenticado
     * @param request DTO con información del pedido (ej. dirección)
     * @return El pedido recién creado
     */
    PedidoResponse crearPedidoDesdeCarrito(String userEmail, PedidoRequest request);

    /**
     * Obtiene todos los pedidos del usuario autenticado.
     * @param userEmail Email del usuario autenticado
     * @return Lista de sus pedidos
     */
    List<PedidoResponse> getPedidosByUsuario(String userEmail);

    /**
     * Obtiene un pedido específico por ID, validando que pertenezca al usuario.
     * @param userEmail Email del usuario autenticado
     * @param pedidoId ID del pedido a buscar
     * @return El pedido
     */
    PedidoResponse getPedidoById(String userEmail, Integer pedidoId);
}
