package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;

public interface CarritoService {

    /**
     * Obtiene el carrito del usuario actualmente autenticado.
     * @param userEmail Email del usuario (obtenido del token JWT)
     * @return El carrito del usuario
     */
    CarritoResponse getCarritoByUsuario(String userEmail);

    /**
     * Añade un item al carrito del usuario autenticado.
     * Si el producto ya existe, actualiza la cantidad.
     * Valida el stock antes de añadir/actualizar.
     * @param userEmail Email del usuario
     * @param request DTO con productoId y cantidad
     * @return El carrito actualizado
     * @throws RuntimeException Si el stock es insuficiente o la cantidad es inválida.
     * @throws jakarta.persistence.EntityNotFoundException Si el usuario o producto no se encuentran.
     */
    CarritoResponse addItemToCarrito(String userEmail, AddItemRequest request);

    /**
     * Elimina un item del carrito del usuario.
     * @param userEmail Email del usuario
     * @param detalleCarritoId ID del item a eliminar (no el ID del producto)
     * @return El carrito actualizado
     * @throws SecurityException Si el item no pertenece al carrito del usuario.
     * @throws jakarta.persistence.EntityNotFoundException Si el usuario, carrito o item no se encuentran.
     */
    CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId);
}
