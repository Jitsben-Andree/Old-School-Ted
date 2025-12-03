package com.example.OldSchoolTeed.service.impl;



import com.example.OldSchoolTeed.dto.CategoriaRequest;
import com.example.OldSchoolTeed.dto.CategoriaResponse;
import com.example.OldSchoolTeed.entities.Categoria;
import com.example.OldSchoolTeed.repository.CategoriaRepository;
import com.example.OldSchoolTeed.service.CategoriaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    //  Lógica de Mapeo
    private CategoriaResponse mapToCategoriaResponse(Categoria categoria) {
        return CategoriaResponse.builder()
                .idCategoria(categoria.getIdCategoria())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .build();
    }

    @Override
    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest request) {
        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());

        Categoria categoriaGuardada = categoriaRepository.save(categoria);
        return mapToCategoriaResponse(categoriaGuardada);
    }

    @Override
    @Transactional
    public CategoriaResponse actualizarCategoria(Integer id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));

        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());

        Categoria categoriaActualizada = categoriaRepository.save(categoria);
        return mapToCategoriaResponse(categoriaActualizada);
    }

    @Override
    @Transactional
    public void eliminarCategoria(Integer id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));

        // Opcional: Validar si la categoría está siendo usada por algún producto antes de borrar
        // if (productoRepository.existsByCategoria(categoria)) {
        //     throw new RuntimeException("No se puede eliminar la categoría, está en uso.");
        // }

        categoriaRepository.delete(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse getCategoriaById(Integer id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));
        return mapToCategoriaResponse(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> getAllCategorias() {
        return categoriaRepository.findAll().stream()
                .map(this::mapToCategoriaResponse)
                .collect(Collectors.toList());
    }
}