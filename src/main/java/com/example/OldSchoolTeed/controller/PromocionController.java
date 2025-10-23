package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.PromocionRequest;
import com.example.OldSchoolTeed.dto.PromocionResponse;
import com.example.OldSchoolTeed.service.PromocionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promociones") // Ruta base (recuerda que el context-path es /api/v1)
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    // --- Endpoints Públicos (para Clientes) ---

    @GetMapping
    public ResponseEntity<List<PromocionResponse>> obtenerTodasLasPromociones() {
        // Opcional: Podrías filtrar para mostrar solo las activas
        return ResponseEntity.ok(promocionService.getAllPromociones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocionResponse> obtenerPromocionPorId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(promocionService.getPromocionById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // --- Endpoints de Administrador ---
    @PostMapping
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<PromocionResponse> crearPromocion(@Valid @RequestBody PromocionRequest request) {
        PromocionResponse promocion = promocionService.crearPromocion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(promocion);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<PromocionResponse> actualizarPromocion(@PathVariable Integer id, @Valid @RequestBody PromocionRequest request) {
        try {
            PromocionResponse promocion = promocionService.actualizarPromocion(id, request);
            return ResponseEntity.ok(promocion);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Void> desactivarPromocion(@PathVariable Integer id) {
        try {
            promocionService.desactivarPromocion(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}