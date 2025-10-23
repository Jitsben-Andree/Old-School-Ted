package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.PromocionRequest;
import com.example.OldSchoolTeed.dto.PromocionResponse;

import java.util.List;

public interface PromocionService {

    /**
     * Crea una nueva promoción.
     * @param request DTO con la información de la nueva promoción.
     * @return La promoción creada.
     */
    PromocionResponse crearPromocion(PromocionRequest request);

    /**
     * Actualiza una promoción existente.
     * @param id El ID de la promoción a actualizar.
     * @param request DTO con la nueva información.
     * @return La promoción actualizada.
     */
    PromocionResponse actualizarPromocion(Integer id, PromocionRequest request);

    /**
     * Desactiva (borrado lógico) una promoción por su ID.
     * @param id El ID de la promoción a desactivar.
     */
    void desactivarPromocion(Integer id);

    /**
     * Obtiene una promoción por su ID.
     * @param id El ID de la promoción.
     * @return La promoción.
     */
    PromocionResponse getPromocionById(Integer id);

    /**
     * Obtiene una lista de todas las promociones.
     * @return Lista de promociones.
     */
    List<PromocionResponse> getAllPromociones();
}