package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.CategoriaRequest;
import com.example.OldSchoolTeed.dto.CategoriaResponse;

import java.util.List;

public interface CategoriaService {


    CategoriaResponse crearCategoria(CategoriaRequest request);


    CategoriaResponse actualizarCategoria(Integer id, CategoriaRequest request);


    void eliminarCategoria(Integer id);

    CategoriaResponse getCategoriaById(Integer id);

    List<CategoriaResponse> getAllCategorias();
}