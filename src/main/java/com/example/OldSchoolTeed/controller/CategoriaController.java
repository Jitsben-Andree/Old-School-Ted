package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.CategoriaRequest;
import com.example.OldSchoolTeed.dto.CategoriaResponse;
import com.example.OldSchoolTeed.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias") // Ruta base (recuerda que el context-path es /api/v1)
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // --- Endpoints Públicos (para Clientes) ---

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> obtenerTodasLasCategorias() {
        return ResponseEntity.ok(categoriaService.getAllCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> obtenerCategoriaPorId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(categoriaService.getCategoriaById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // --- Endpoints de Administrador ---

    @PostMapping
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<CategoriaResponse> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse categoria = categoriaService.crearCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<CategoriaResponse> actualizarCategoria(@PathVariable Integer id, @Valid @RequestBody CategoriaRequest request) {
        try {
            CategoriaResponse categoria = categoriaService.actualizarCategoria(id, request);
            return ResponseEntity.ok(categoria);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        try {
            categoriaService.eliminarCategoria(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}