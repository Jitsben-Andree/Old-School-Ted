package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.CategoriaRequest;
import com.example.OldSchoolTeed.dto.CategoriaResponse;
import com.example.OldSchoolTeed.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@Slf4j
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    //  Endpoints Públicos

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> obtenerTodasLasCategorias() {
        log.info("GET /categorias -> Listando todas");
        return ResponseEntity.ok(categoriaService.getAllCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> obtenerCategoriaPorId(@PathVariable Integer id) {
        log.info("GET /categorias/{}", id);
        return ResponseEntity.ok(categoriaService.getCategoriaById(id));
    }

    //  Endpoints de Administrador

    @PostMapping
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<CategoriaResponse> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        log.info("Admin: POST /categorias -> Creando: {}", request.getNombre());
        CategoriaResponse categoria = categoriaService.crearCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<CategoriaResponse> actualizarCategoria(@PathVariable Integer id, @Valid @RequestBody CategoriaRequest request) {
        log.info("Admin: PUT /categorias/{}", id);
        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        log.info("Admin: DELETE /categorias/{}", id);
        categoriaService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}