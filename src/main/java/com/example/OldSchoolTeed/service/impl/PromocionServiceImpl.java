package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.PromocionRequest;
import com.example.OldSchoolTeed.dto.PromocionResponse;
import com.example.OldSchoolTeed.entities.Promocion;
import com.example.OldSchoolTeed.repository.PromocionRepository;
import com.example.OldSchoolTeed.service.PromocionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromocionServiceImpl implements PromocionService {

    private final PromocionRepository promocionRepository;

    public PromocionServiceImpl(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    //  Lógica de Mapeo (Helper)
    private PromocionResponse mapToPromocionResponse(Promocion promocion) {
        return PromocionResponse.builder()
                .idPromocion(promocion.getIdPromocion())
                .codigo(promocion.getCodigo())
                .descripcion(promocion.getDescripcion())
                .descuento(promocion.getDescuento())
                .fechaInicio(promocion.getFechaInicio())
                .fechaFin(promocion.getFechaFin())
                .activa(promocion.isActiva())
                .build();
    }

    @Override
    @Transactional
    public PromocionResponse crearPromocion(PromocionRequest request) {
        Promocion promocion = new Promocion();
        promocion.setCodigo(request.getCodigo());
        promocion.setDescripcion(request.getDescripcion());
        promocion.setDescuento(request.getDescuento());
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActiva(request.isActiva());

        Promocion promocionGuardada = promocionRepository.save(promocion);
        return mapToPromocionResponse(promocionGuardada);
    }

    @Override
    @Transactional
    public PromocionResponse actualizarPromocion(Integer id, PromocionRequest request) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + id));

        promocion.setCodigo(request.getCodigo());
        promocion.setDescripcion(request.getDescripcion());
        promocion.setDescuento(request.getDescuento());
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActiva(request.isActiva());

        Promocion promocionActualizada = promocionRepository.save(promocion);
        return mapToPromocionResponse(promocionActualizada);
    }

    @Override
    @Transactional
    public void desactivarPromocion(Integer id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + id));

        // Borrado lógico en lugar de físico
        promocion.setActiva(false);
        promocionRepository.save(promocion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromocionResponse getPromocionById(Integer id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + id));
        return mapToPromocionResponse(promocion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionResponse> getAllPromociones() {
        return promocionRepository.findAll().stream()
                .map(this::mapToPromocionResponse)
                .collect(Collectors.toList());
    }
}