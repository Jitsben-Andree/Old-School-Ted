package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.InventarioResponse;
import com.example.OldSchoolTeed.dto.InventarioUpdateRequest;

import java.util.List;

public interface InventarioService {


        InventarioResponse actualizarStock(InventarioUpdateRequest request);

        List<InventarioResponse> getTodoElInventario();

        InventarioResponse getInventarioPorProductoId(Integer productoId);
}


