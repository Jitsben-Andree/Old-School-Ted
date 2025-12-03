package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.PromocionRequest;
import com.example.OldSchoolTeed.dto.PromocionResponse;
import com.example.OldSchoolTeed.service.PromocionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    //  Endpoints Públicos

    @GetMapping("/promociones")
    public ResponseEntity<List<PromocionResponse>> obtenerTodasLasPromociones() {
        log.info("GET /promociones -> Listando todas (público)");
        return ResponseEntity.ok(promocionService.getAllPromociones());
    }

    @GetMapping("/promociones/{id}")
    public ResponseEntity<PromocionResponse> obtenerPromocionPorId(@PathVariable Integer id) {
        log.info("GET /promociones/{} (público)", id);
        return ResponseEntity.ok(promocionService.getPromocionById(id));
    }

    //  Endpoints de Administrador

    @PostMapping("/admin/promociones")
    public ResponseEntity<PromocionResponse> crearPromocionAdmin(@Valid @RequestBody PromocionRequest request) {
        log.info("Admin: POST /admin/promociones -> Codigo: {}", request.getCodigo());
        PromocionResponse promocion = promocionService.crearPromocion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(promocion);
    }

    @PutMapping("/admin/promociones/{id}")
    public ResponseEntity<PromocionResponse> actualizarPromocionAdmin(@PathVariable Integer id, @Valid @RequestBody PromocionRequest request) {
        log.info("Admin: PUT /admin/promociones/{}", id);
        return ResponseEntity.ok(promocionService.actualizarPromocion(id, request));
    }

    @DeleteMapping("/admin/promociones/{id}")
    public ResponseEntity<Void> desactivarPromocionAdmin(@PathVariable Integer id) {
        log.info("Admin: DELETE /admin/promociones/{}", id);
        promocionService.desactivarPromocion(id);
        return ResponseEntity.noContent().build();
    }
}