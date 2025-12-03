package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.*;

import java.util.List;

public interface PedidoService {

    // --- MÉTODOS DE CLIENTE ---

    PedidoResponse crearPedidoDesdeCarrito(String userEmail, PedidoRequest request);

    List<PedidoResponse> getPedidosByUsuario(String userEmail);

    PedidoResponse getPedidoById(String userEmail, Integer pedidoId);


    // --- MÉTODOS DE ADMINISTRADOR ---

    List<PedidoResponse> getAllPedidosAdmin();

    PedidoResponse updatePedidoStatusAdmin(Integer pedidoId, AdminUpdatePedidoStatusRequest request);

    PedidoResponse updatePagoStatusAdmin(Integer pedidoId, AdminUpdatePagoRequest request);

    PedidoResponse updateEnvioDetailsAdmin(Integer pedidoId, AdminUpdateEnvioRequest request);

    void deletePedido(Integer pedidoId);
}