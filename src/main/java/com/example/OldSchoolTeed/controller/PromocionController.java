package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.PromocionRequest;
import com.example.OldSchoolTeed.dto.PromocionResponse;
import com.example.OldSchoolTeed.service.PromocionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Quitar PreAuthorize de aquí si ya no se usa a nivel de método
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
// NO usar RequestMapping global aquí si tenemos rutas públicas y privadas
// @RequestMapping("/promociones")
public class PromocionController {

    private static final Logger log = LoggerFactory.getLogger(PromocionController.class);
    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    // --- Endpoints Públicos (Se quedan en /promociones) ---

    @GetMapping("/promociones") // Ruta completa
    public ResponseEntity<List<PromocionResponse>> obtenerTodasLasPromociones() {
        log.info("GET /promociones -> Obteniendo todas las promociones (público)");
        try {
            return ResponseEntity.ok(promocionService.getAllPromociones());
        } catch (Exception e) {
            log.error("Error al obtener todas las promociones", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener promociones", e);
        }
    }

    @GetMapping("/promociones/{id}") // Ruta completa
    public ResponseEntity<PromocionResponse> obtenerPromocionPorId(@PathVariable Integer id) {
        log.info("GET /promociones/{} -> Obteniendo promoción por ID (público)", id);
        try {
            return ResponseEntity.ok(promocionService.getPromocionById(id));
        } catch (EntityNotFoundException e) {
            log.warn("Promoción no encontrada con ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al obtener promoción ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener promoción", e);
        }
    }

    // --- Endpoints de Administrador (Ahora bajo /admin/promociones) ---
    // La seguridad la maneja SecurityConfig con .requestMatchers("/admin/**")

    @PostMapping("/admin/promociones") // <<< NUEVA RUTA
    // @PreAuthorize("hasAuthority('Administrador')") // Ya no es necesario aquí
    public ResponseEntity<PromocionResponse> crearPromocionAdmin(@Valid @RequestBody PromocionRequest request) {
        log.info("Admin: POST /admin/promociones -> Intentando crear promoción con código: {}", request.getCodigo());
        try {
            PromocionResponse promocion = promocionService.crearPromocion(request);
            log.info("Admin: Promoción creada con ID: {}", promocion.getIdPromocion());
            return ResponseEntity.status(HttpStatus.CREATED).body(promocion);
        } catch (Exception e) {
            log.error("Admin: Error al crear promoción", e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("constraint") && e.getMessage().toLowerCase().contains("codigo")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una promoción con ese código.", e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear promoción", e);
        }
    }


    @PutMapping("/admin/promociones/{id}") // <<< NUEVA RUTA
    // @PreAuthorize("hasAuthority('Administrador')") // Ya no es necesario aquí
    public ResponseEntity<PromocionResponse> actualizarPromocionAdmin(@PathVariable Integer id, @Valid @RequestBody PromocionRequest request) {
        log.info("Admin: PUT /admin/promociones/{} -> Actualizando promoción", id);
        try {
            PromocionResponse promocion = promocionService.actualizarPromocion(id, request);
            log.info("Admin: Promoción ID {} actualizada.", id);
            return ResponseEntity.ok(promocion);
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al actualizar, promoción no encontrada ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al actualizar promoción ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar promoción", e);
        }
    }

    @DeleteMapping("/admin/promociones/{id}") // <<< NUEVA RUTA
    // @PreAuthorize("hasAuthority('Administrador')") // Ya no es necesario aquí
    public ResponseEntity<Void> desactivarPromocionAdmin(@PathVariable Integer id) {
        log.info("Admin: DELETE /admin/promociones/{} -> Desactivando promoción", id);
        try {
            promocionService.desactivarPromocion(id);
            log.info("Admin: Promoción ID {} desactivada.", id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.warn("Admin: Error al desactivar, promoción no encontrada ID: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Admin: Error inesperado al desactivar promoción ID {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al desactivar promoción", e);
        }
    }
}

