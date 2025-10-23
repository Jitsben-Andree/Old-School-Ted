package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.InventarioResponse;
import com.example.OldSchoolTeed.dto.InventarioUpdateRequest;

import java.util.List;

public interface InventarioService {

        /**
         * [Admin] Actualiza el stock de un producto específico.
         * @param request DTO con el productoId y el nuevoStock.
         * @return El InventarioResponse actualizado.
         */
        InventarioResponse actualizarStock(InventarioUpdateRequest request);

        /**
         * [Admin] Obtiene el estado del inventario de todos los productos.
         * @return Lista del inventario de todos los productos.
         */
        List<InventarioResponse> getTodoElInventario();

        /**
         * [Admin] Obtiene el inventario de un producto específico.
         * @param productoId ID del producto a consultar.
         * @return El InventarioResponse.
         */
        InventarioResponse getInventarioPorProductoId(Integer productoId);
}


