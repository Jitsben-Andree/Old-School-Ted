package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.UpdateCantidadRequest;
import jakarta.validation.Valid;

public interface CarritoService {

    CarritoResponse getCarritoByUsuario(String userEmail);


    CarritoResponse addItemToCarrito(String userEmail, @Valid AddItemRequest request);


    CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId);

    CarritoResponse updateItemQuantity(String userEmail, Integer detalleCarritoId, @Valid UpdateCantidadRequest request);
}