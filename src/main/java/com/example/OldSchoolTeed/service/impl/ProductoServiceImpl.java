package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.entities.Categoria;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.entities.Promocion; // Importar Promocion
import com.example.OldSchoolTeed.repository.CategoriaRepository;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.repository.PromocionRepository; // Importar PromocionRepository
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // Importar BigDecimal
import java.math.RoundingMode; // Importar RoundingMode
import java.time.LocalDateTime; // Importar LocalDateTime
import java.util.Comparator; // Importar Comparator
import java.util.List;
import java.util.Optional; // Importar Optional
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class); // Añadir logger
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioRepository inventarioRepository;
    private final PromocionRepository promocionRepository; // Inyectar PromocionRepository

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               InventarioRepository inventarioRepository,
                               PromocionRepository promocionRepository) { // Añadir al constructor
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.promocionRepository = promocionRepository; // Asignar
    }

    // --- Lógica de Mapeo (Helper) ---
    private ProductoResponse mapToProductoResponse(Producto producto) {
        log.trace("Mapeando Producto ID: {}", producto.getIdProducto());
        // Buscamos el inventario para este producto
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> {
                    log.warn("No se encontró inventario para Producto ID: {}, devolviendo stock 0.", producto.getIdProducto());
                    Inventario tempInv = new Inventario();
                    tempInv.setStock(0);
                    tempInv.setProducto(producto);
                    return tempInv;
                });

        // --- Lógica de Cálculo de Promociones ---
        BigDecimal precioOriginal = producto.getPrecio();
        BigDecimal precioConDescuento = precioOriginal; // Por defecto, es el mismo
        BigDecimal descuentoAplicado = BigDecimal.ZERO;
        String nombrePromocion = null;

        // Buscar promociones activas para este producto AHORA
        List<Promocion> promocionesActivas = promocionRepository.findActivePromocionesForProducto(producto.getIdProducto(), LocalDateTime.now());
        log.trace("Encontradas {} promociones activas para Producto ID: {}", promocionesActivas.size(), producto.getIdProducto());


        if (!promocionesActivas.isEmpty()) {
            // Lógica para elegir la "mejor" promoción (ej. la de mayor descuento)
            Optional<Promocion> mejorPromocionOpt = promocionesActivas.stream()
                    .max(Comparator.comparing(Promocion::getDescuento)); // Comparar por el campo 'descuento'

            if (mejorPromocionOpt.isPresent()) {
                Promocion mejorPromocion = mejorPromocionOpt.get();
                descuentoAplicado = mejorPromocion.getDescuento(); // Asumimos que es porcentaje
                nombrePromocion = mejorPromocion.getDescripcion(); // O getCodigo()

                // Calcular el descuento (asumiendo que es porcentaje)
                // descuento = precio * (porcentaje / 100)
                BigDecimal factorDescuento = descuentoAplicado.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal montoDescuento = precioOriginal.multiply(factorDescuento);
                precioConDescuento = precioOriginal.subtract(montoDescuento).setScale(2, RoundingMode.HALF_UP);

                log.debug("Promoción aplicada a Producto ID {}: '{}' ({}%). Precio: {} -> {}",
                        producto.getIdProducto(), nombrePromocion, descuentoAplicado, precioOriginal, precioConDescuento);

            }
        }
        // --- Fin Lógica de Promociones ---


        return ProductoResponse.builder()
                .id(producto.getIdProducto())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .talla(producto.getTalla() != null ? producto.getTalla().name() : "N/A") // Manejar null
                .precio(precioConDescuento) // <<< PRECIO FINAL CON DESCUENTO
                .activo(producto.getActivo())
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin Categoría") // Manejar null
                .stock(inventario.getStock())
                .imageUrl(producto.getImageUrl())
                // Nuevos campos
                .precioOriginal(precioOriginal) // <<< PRECIO BASE
                .descuentoAplicado(descuentoAplicado)
                .nombrePromocion(nombrePromocion)
                .build();
    }


    // --- Implementación de Métodos Públicos ---

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosActivos() {
        log.info("Obteniendo todos los productos activos.");
        return productoRepository.findByActivoTrue().stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    // --- NUEVO MÉTODO PARA ADMIN ---
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosIncludingInactive() {
        log.info("Admin: Obteniendo todos los productos (activos e inactivos).");
        return productoRepository.findAll().stream() // Usar findAll() en lugar de findByActivoTrue()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Integer id) {
        log.info("Obteniendo producto por ID: {}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
        return mapToProductoResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(String nombreCategoria) {
        log.info("Obteniendo productos por categoría: {}", nombreCategoria);
        return productoRepository.findByCategoriaNombre(nombreCategoria).stream()
                .filter(Producto::getActivo) // Aseguramos que solo devuelva activos
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    // --- Implementación de Métodos de Administrador ---

    @Override
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        log.info("Admin: Creando nuevo producto con nombre: {}", request.getNombre());
        // 1. Buscar la categoría
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        // 2. Crear el producto
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        // Convertir String a Enum (manejando posible error)
        try {
            producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Talla inválida recibida: '{}'. Usando 'M' por defecto.", request.getTalla(), e);
            producto.setTalla(Producto.Talla.M); // O lanzar error
        }

        producto.setPrecio(request.getPrecio());
        producto.setActivo(request.getActivo() != null ? request.getActivo() : true);
        producto.setCategoria(categoria);

        Producto productoGuardado = productoRepository.save(producto);
        log.info("Admin: Producto creado con ID: {}", productoGuardado.getIdProducto());


        // 3. Crear su registro de inventario inicial (con stock 0)
        // Verificar si ya existe inventario por si acaso (aunque no debería)
        if (!inventarioRepository.findByProducto(productoGuardado).isPresent()) {
            Inventario inventario = new Inventario();
            inventario.setProducto(productoGuardado);
            inventario.setStock(0);
            inventarioRepository.save(inventario);
            log.info("Admin: Inventario inicial creado para Producto ID: {}", productoGuardado.getIdProducto());
        } else {
            log.warn("Admin: Ya existía inventario para Producto ID: {}", productoGuardado.getIdProducto());
        }


        return mapToProductoResponse(productoGuardado);
    }

    @Override
    @Transactional
    public ProductoResponse updateProducto(Integer id, ProductoRequest request) {
        log.info("Admin: Actualizando producto ID: {}", id);
        // 1. Buscar el producto existente
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        // 2. Buscar la categoría si cambió
        if (request.getCategoriaId() != null && (producto.getCategoria() == null || !request.getCategoriaId().equals(producto.getCategoria().getIdCategoria()))) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
            log.debug("Admin: Categoría actualizada para Producto ID: {}", id);
        }

        // 3. Actualizar campos
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        // Convertir String a Enum (manejando posible error)
        try {
            producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Talla inválida recibida al actualizar Producto ID {}: '{}'. Manteniendo talla anterior.", id, request.getTalla(), e);
            // No cambiar la talla si es inválida
        }
        producto.setPrecio(request.getPrecio());
        // Manejar el caso donde activo es null en el request
        producto.setActivo(request.getActivo() != null ? request.getActivo() : producto.getActivo());


        Producto productoActualizado = productoRepository.save(producto);
        log.info("Admin: Producto ID {} actualizado.", id);
        return mapToProductoResponse(productoActualizado);
    }

    @Override
    @Transactional
    public void deleteProducto(Integer id) {
        log.info("Admin: Desactivando (soft delete) producto ID: {}", id);
        // Implementamos Soft Delete (Borrado Lógico)
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Admin: Producto ID {} desactivado.", id);

    }

    // --- NUEVO MÉTODO PARA ASOCIAR PROMOCIONES ---
    @Override
    @Transactional
    public void associatePromocionToProducto(Integer productoId, Integer promocionId) {
        log.info("Admin: Asociando Promoción ID {} a Producto ID {}", promocionId, productoId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + promocionId));

        // Añadir la promoción al Set del producto
        producto.getPromociones().add(promocion);
        productoRepository.save(producto);
        log.info("Admin: Asociación completada.");
    }

    @Override
    @Transactional
    public void disassociatePromocionFromProducto(Integer productoId, Integer promocionId) {
        log.info("Admin: Desasociando Promoción ID {} de Producto ID {}", promocionId, productoId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + promocionId));

        // Quitar la promoción del Set del producto
        producto.getPromociones().remove(promocion);
        productoRepository.save(producto);
        log.info("Admin: Desasociación completada.");
    }


}

