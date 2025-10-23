package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.entities.Categoria;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.repository.CategoriaRepository;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioRepository inventarioRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               InventarioRepository inventarioRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
    }

    // --- Lógica de Mapeo (Helper) ---
    private ProductoResponse mapToProductoResponse(Producto producto) {
        // Buscamos el inventario para este producto
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> {
                    // FIX: Creamos un inventario temporal para el DTO si no existe
                    Inventario tempInv = new Inventario();
                    tempInv.setStock(0);
                    tempInv.setProducto(producto); // Asignamos el producto para evitar NullPointer
                    return tempInv;
                });

        return ProductoResponse.builder()
                .id(producto.getIdProducto()) // Asumo que corregiste esto a idProducto
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .talla(producto.getTalla().name()) // Asumo que corregiste esto a .name()
                .precio(producto.getPrecio())
                .activo(producto.getActivo())
                .categoriaNombre(producto.getCategoria().getNombre())
                .stock(inventario.getStock())
                .build();
    }


    // --- Implementación de Métodos Públicos ---

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosActivos() {
        return productoRepository.findByActivoTrue().stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Integer id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
        return mapToProductoResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(String nombreCategoria) {
        return productoRepository.findByCategoriaNombre(nombreCategoria).stream()
                .filter(Producto::getActivo) // Aseguramos que solo devuelva activos
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    // --- Implementación de Métodos de Administrador ---

    @Override
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        // 1. Buscar la categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        // 2. Crear el producto
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        // FIX: Convertir String a Enum (asumo que 'Talla' está dentro de 'Producto')
        producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
        producto.setPrecio(request.getPrecio());
        producto.setActivo(request.getActivo() != null ? request.getActivo() : true);
        producto.setCategoria(categoria);

        Producto productoGuardado = productoRepository.save(producto);

        // 3. Crear su registro de inventario inicial (con stock 0)
        // ---- FIX AQUÍ ----
        Inventario inventario = new Inventario(); // Usar constructor vacío
        inventario.setProducto(productoGuardado);
        inventario.setStock(0);
        // la fecha 'ultimaActualizacion' se setea sola por @PrePersist
        inventarioRepository.save(inventario);

        return mapToProductoResponse(productoGuardado);
    }

    @Override
    @Transactional
    public ProductoResponse updateProducto(Integer id, ProductoRequest request) {
        // 1. Buscar el producto existente
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        // 2. Buscar la categoría si cambió
        // Asumo que corregiste getId() a getIdCategoria()
        if (request.getCategoriaId() != null && !request.getCategoriaId().equals(producto.getCategoria().getIdCategoria())) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
        }

        // 3. Actualizar campos
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        // FIX: Convertir String a Enum
        producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
        producto.setPrecio(request.getPrecio());
        producto.setActivo(request.getActivo());

        Producto productoActualizado = productoRepository.save(producto);
        return mapToProductoResponse(productoActualizado);
    }

    @Override
    @Transactional
    public void deleteProducto(Integer id) {
        // Implementamos Soft Delete (Borrado Lógico)
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
    }
}