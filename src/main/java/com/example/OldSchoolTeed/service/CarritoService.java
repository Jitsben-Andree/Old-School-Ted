package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.UpdateCantidadRequest;
import jakarta.validation.Valid;

public interface CarritoService {

    /**
     * Obtiene el carrito actual de un usuario basado en su email.
     * Si no existe, crea uno nuevo vacío.
     */
    CarritoResponse getCarritoByUsuario(String userEmail);

    /**
     * Agrega un ítem al carrito. Maneja la lógica de stock, creación de
     * nuevas líneas para productos personalizados o agrupación para productos idénticos.
     */
    CarritoResponse addItemToCarrito(String userEmail, @Valid AddItemRequest request);

    /**
     * Elimina un ítem específico (línea de detalle) del carrito del usuario.
     * Verifica que el ítem pertenezca al usuario antes de borrarlo.
     */
    CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId);

    /**
     * Actualiza la cantidad de un producto ya existente en el carrito.
     * Valida el stock disponible antes de permitir el aumento.
     */
    CarritoResponse updateItemQuantity(String userEmail, Integer detalleCarritoId, @Valid UpdateCantidadRequest request);
}