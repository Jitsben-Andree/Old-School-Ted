package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProductoProveedorRequest;
import com.example.OldSchoolTeed.dto.ProductoProveedorResponse;
import com.example.OldSchoolTeed.dto.UpdatePrecioCostoRequest;
import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.entities.ProductoProveedor;
import com.example.OldSchoolTeed.entities.Proveedor;
import com.example.OldSchoolTeed.repository.ProductoProveedorRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.repository.ProveedorRepository;
import com.example.OldSchoolTeed.service.ProductoProveedorService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoProveedorServiceImpl implements ProductoProveedorService {

    private final ProductoProveedorRepository productoProveedorRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoProveedorServiceImpl(ProductoProveedorRepository productoProveedorRepository,
                                        ProductoRepository productoRepository,
                                        ProveedorRepository proveedorRepository) {
        this.productoProveedorRepository = productoProveedorRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    @Transactional
    public ProductoProveedorResponse createAsignacion(ProductoProveedorRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductoId()));

        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + request.getProveedorId()));


        productoProveedorRepository.findByProductoAndProveedor(producto, proveedor)
                .ifPresent(asignacionExistente -> {
                    throw new RuntimeException("Este producto ya está asignado a este proveedor.");
                });

        ProductoProveedor nuevaAsignacion = new ProductoProveedor();
        nuevaAsignacion.setProducto(producto);
        nuevaAsignacion.setProveedor(proveedor);
        nuevaAsignacion.setPrecioCosto(request.getPrecioCosto());

        ProductoProveedor asignacionGuardada = productoProveedorRepository.save(nuevaAsignacion);
        return mapToResponse(asignacionGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoProveedorResponse> getAsignacionesPorProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));

        return productoProveedorRepository.findByProducto(producto).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoProveedorResponse> getAsignacionesPorProveedor(Integer proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + proveedorId));

        return productoProveedorRepository.findByProveedor(proveedor).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoProveedorResponse updatePrecioCosto(Integer asignacionId, UpdatePrecioCostoRequest request) {
        ProductoProveedor asignacion = productoProveedorRepository.findById(asignacionId)
                .orElseThrow(() -> new EntityNotFoundException("Asignación no encontrada con ID: " + asignacionId));

        asignacion.setPrecioCosto(request.getNuevoPrecioCosto());
        ProductoProveedor asignacionActualizada = productoProveedorRepository.save(asignacion);
        return mapToResponse(asignacionActualizada);
    }

    @Override
    @Transactional
    public void deleteAsignacion(Integer asignacionId) {
        if (!productoProveedorRepository.existsById(asignacionId)) {
            throw new EntityNotFoundException("Asignación no encontrada con ID: " + asignacionId);
        }
        productoProveedorRepository.deleteById(asignacionId);
    }


    private ProductoProveedorResponse mapToResponse(ProductoProveedor asignacion) {
        return ProductoProveedorResponse.builder()
                .idAsignacion(asignacion.getIdProdProv())
                .productoId(asignacion.getProducto().getIdProducto())
                .productoNombre(asignacion.getProducto().getNombre())
                .proveedorId(asignacion.getProveedor().getIdProveedor())
                .proveedorRazonSocial(asignacion.getProveedor().getRazonSocial())
                .precioCosto(asignacion.getPrecioCosto())
                .build();
    }
}