package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.PromocionRequest;
import com.example.OldSchoolTeed.dto.PromocionResponse;

import java.util.List;

public interface PromocionService {


    PromocionResponse crearPromocion(PromocionRequest request);


    PromocionResponse actualizarPromocion(Integer id, PromocionRequest request);


    void desactivarPromocion(Integer id);


    PromocionResponse getPromocionById(Integer id);


    List<PromocionResponse> getAllPromociones();
}