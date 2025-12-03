package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProveedorRequest;
import com.example.OldSchoolTeed.dto.ProveedorResponse;
import com.example.OldSchoolTeed.entities.Proveedor;
import com.example.OldSchoolTeed.repository.ProductoProveedorRepository;
import com.example.OldSchoolTeed.repository.ProveedorRepository;
import com.example.OldSchoolTeed.service.ProveedorService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoProveedorRepository productoProveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository, ProductoProveedorRepository productoProveedorRepository) {
        this.proveedorRepository = proveedorRepository;
        this.productoProveedorRepository = productoProveedorRepository;
    }

    @Override
    @Transactional
    public ProveedorResponse createProveedor(ProveedorRequest request) {
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial(request.getRazonSocial());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());


        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);
        return mapToResponse(proveedorGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse getProveedorById(Integer id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));
        return mapToResponse(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> getAllProveedores() {
        return proveedorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProveedorResponse updateProveedor(Integer id, ProveedorRequest request) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));

        proveedor.setRazonSocial(request.getRazonSocial());
        proveedor.setContacto(request.getContacto());
        proveedor.setTelefono(request.getTelefono());
        proveedor.setDireccion(request.getDireccion());


        Proveedor proveedorActualizado = proveedorRepository.save(proveedor);
        return mapToResponse(proveedorActualizado);
    }

    @Override
    @Transactional
    public void deleteProveedor(Integer id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado con ID: " + id));


        boolean isAssociated = productoProveedorRepository.existsByProveedor(proveedor);
        if (isAssociated) {
            throw new RuntimeException("No se puede eliminar el proveedor (ID: " + id + ") porque está asociado a uno o más productos.");
        }

        proveedorRepository.delete(proveedor);
    }

    private ProveedorResponse mapToResponse(Proveedor proveedor) {
        return ProveedorResponse.builder()
                .idProveedor(proveedor.getIdProveedor())
                .razonSocial(proveedor.getRazonSocial())
                .contacto(proveedor.getContacto())
                .telefono(proveedor.getTelefono())
                .direccion(proveedor.getDireccion())
                // LÍNEA DE PRECIO ELIMINADA
                .build();
    }
}
