package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.CategoriaRequest;
import com.example.OldSchoolTeed.dto.CategoriaResponse;

import java.util.List;

public interface CategoriaService {

    /**
     * Crea una nueva categoría.
     * @param request DTO con la información de la nueva categoría.
     * @return La categoría creada.
     */
    CategoriaResponse crearCategoria(CategoriaRequest request);

    /**
     * Actualiza una categoría existente.
     * @param id El ID de la categoría a actualizar.
     * @param request DTO con la nueva información.
     * @return La categoría actualizada.
     */
    CategoriaResponse actualizarCategoria(Integer id, CategoriaRequest request);

    /**
     * Elimina una categoría por su ID.
     * @param id El ID de la categoría a eliminar.
     */
    void eliminarCategoria(Integer id);

    /**
     * Obtiene una categoría por su ID.
     * @param id El ID de la categoría.
     * @return La categoría.
     */
    CategoriaResponse getCategoriaById(Integer id);

    /**
     * Obtiene una lista de todas las categorías.
     * @return Lista de categorías.
     */
    List<CategoriaResponse> getAllCategorias();
}