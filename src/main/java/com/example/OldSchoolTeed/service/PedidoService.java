package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.*;

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

    // --- NUEVOS MÉTODOS DE ADMIN ---

    /**
     * [Admin] Obtiene todos los pedidos de todos los usuarios.
     * @return Lista de todos los PedidoResponse.
     */
    List<PedidoResponse> getAllPedidosAdmin();

    /**
     * [Admin] Actualiza el estado general de un pedido.
     * @param pedidoId ID del pedido a actualizar.
     * @param request DTO con el nuevo estado.
     * @return El PedidoResponse actualizado.
     */
    PedidoResponse updatePedidoStatusAdmin(Integer pedidoId, AdminUpdatePedidoStatusRequest request);

    /**
     * [Admin] Actualiza el estado del pago de un pedido.
     * @param pedidoId ID del pedido cuyo pago se actualizará.
     * @param request DTO con el nuevo estado del pago.
     * @return El PedidoResponse actualizado (reflejando el estado del pago).
     */
    PedidoResponse updatePagoStatusAdmin(Integer pedidoId, AdminUpdatePagoRequest request);

    /**
     * [Admin] Actualiza los detalles del envío de un pedido.
     * @param pedidoId ID del pedido cuyo envío se actualizará.
     * @param request DTO con los detalles del envío.
     * @return El PedidoResponse actualizado (reflejando el estado del envío).
     */
    PedidoResponse updateEnvioDetailsAdmin(Integer pedidoId, AdminUpdateEnvioRequest request);
}
