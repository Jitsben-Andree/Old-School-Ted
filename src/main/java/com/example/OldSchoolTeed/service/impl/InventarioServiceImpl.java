package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.InventarioResponse;
import com.example.OldSchoolTeed.dto.InventarioUpdateRequest;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.service.InventarioService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;

    public InventarioServiceImpl(InventarioRepository inventarioRepository, ProductoRepository productoRepository) {
        this.inventarioRepository = inventarioRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public InventarioResponse actualizarStock(InventarioUpdateRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductoId()));

        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para el producto: " + producto.getNombre()));

        inventario.setStock(request.getNuevoStock());
        inventario.setUltimaActualizacion(LocalDateTime.now());

        Inventario inventarioGuardado = inventarioRepository.save(inventario);

        return mapToInventarioResponse(inventarioGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventarioResponse> getTodoElInventario() {
        return inventarioRepository.findAll().stream()
                .map(this::mapToInventarioResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InventarioResponse getInventarioPorProductoId(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));

        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para el producto: " + producto.getNombre()));

        return mapToInventarioResponse(inventario);
    }

    // Método helper para mapear Entidad a DTO
    private InventarioResponse mapToInventarioResponse(Inventario inventario) {
        return InventarioResponse.builder()
                .inventarioId(inventario.getIdInventario())
                .productoId(inventario.getProducto().getIdProducto())
                .productoNombre(inventario.getProducto().getNombre())
                .stock(inventario.getStock())
                .ultimaActualizacion(inventario.getUltimaActualizacion())
                .build();
    }
}
