package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.dto.PromocionSimpleDto;
import com.example.OldSchoolTeed.entities.Categoria;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.entities.Promocion;
import com.example.OldSchoolTeed.repository.CategoriaRepository;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.repository.PromocionRepository;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioRepository inventarioRepository;
    private final PromocionRepository promocionRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               InventarioRepository inventarioRepository,
                               PromocionRepository promocionRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.promocionRepository = promocionRepository;
    }

    // --- Lógica de Mapeo (Helper) ---
    @Transactional(readOnly = true) // Asegurar transacción para LAZY loading
    private ProductoResponse mapToProductoResponse(Producto producto) {
        log.trace("Mapeando Producto ID: {}", producto.getIdProducto());
        // Inventario
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> {
                    log.warn("No se encontró inventario para Producto ID: {}, devolviendo stock 0.", producto.getIdProducto());
                    Inventario tempInv = new Inventario();
                    tempInv.setStock(0);
                    tempInv.setProducto(producto);
                    return tempInv;
                });

        // --- Lógica de Cálculo de Promociones (REVISADA) ---
        BigDecimal precioOriginal = producto.getPrecio();
        BigDecimal precioConDescuento = precioOriginal; // Por defecto, es el mismo
        BigDecimal descuentoAplicado = BigDecimal.ZERO;
        String nombrePromocion = null;
        Promocion mejorPromocionAplicada = null; // Para guardar la promo elegida

        // Log para ver la hora actual del servidor vs las fechas de la promo
        LocalDateTime now = LocalDateTime.now();
        log.debug("Buscando promociones activas para Producto ID: {} en fecha/hora: {}", producto.getIdProducto(), now);

        // Buscar promociones activas para este producto AHORA
        List<Promocion> promocionesActivas = promocionRepository.findActivePromocionesForProducto(producto.getIdProducto(), now);
        log.debug("Encontradas {} promociones activas para Producto ID: {}", promocionesActivas.size(), producto.getIdProducto());

        // Loguear detalles de las promociones encontradas (si las hay)
        // if (!promocionesActivas.isEmpty()) {
        //     promocionesActivas.forEach(p -> log.trace("Promo activa encontrada ID {}: Desc={}, Inicio={}, Fin={}, Activa={}",
        //         p.getIdPromocion(), p.getDescuento(), p.getFechaInicio(), p.getFechaFin(), p.isActiva()));
        // }


        if (!promocionesActivas.isEmpty()) {
            // Lógica para elegir la "mejor" promoción (la de mayor descuento)
            Optional<Promocion> mejorPromocionOpt = promocionesActivas.stream()
                    .filter(p -> p.getDescuento() != null)
                    .max(Comparator.comparing(Promocion::getDescuento));

            if (mejorPromocionOpt.isPresent()) {
                mejorPromocionAplicada = mejorPromocionOpt.get();
                descuentoAplicado = mejorPromocionAplicada.getDescuento();
                nombrePromocion = mejorPromocionAplicada.getDescripcion();

                log.debug("Mejor promoción encontrada para Prod ID {}: Promo ID {}, Descuento: {}%",
                        producto.getIdProducto(), mejorPromocionAplicada.getIdPromocion(), descuentoAplicado);

                // Validar que el descuento sea un porcentaje válido (0 < desc <= 100)
                if (descuentoAplicado.compareTo(BigDecimal.ZERO) > 0 && descuentoAplicado.compareTo(new BigDecimal("100")) <= 0) {

                    BigDecimal factorDescuento = descuentoAplicado.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal montoDescuento = precioOriginal.multiply(factorDescuento);
                    precioConDescuento = precioOriginal.subtract(montoDescuento).setScale(2, RoundingMode.HALF_UP);

                    // Log CLAVE: Confirmar que el descuento se aplica
                    log.info(">>> Promoción aplicada a Producto ID {}: '{}' ({}%). Precio: {} -> {} <<<",
                            producto.getIdProducto(), nombrePromocion, descuentoAplicado, precioOriginal, precioConDescuento);
                } else {
                    log.warn("Descuento inválido ({}) encontrado para Promoción ID {} en Producto ID {}. No se aplicará descuento.",
                            descuentoAplicado, mejorPromocionAplicada.getIdPromocion(), producto.getIdProducto());
                    descuentoAplicado = BigDecimal.ZERO;
                    nombrePromocion = null;
                    precioConDescuento = precioOriginal;
                }

            } else {
                log.debug("No se encontró una promoción válida (con descuento > 0) para Producto ID {}", producto.getIdProducto());
            }
        } else {
            log.debug("No hay promociones activas en este momento para Producto ID {}", producto.getIdProducto());
        }
        // --- Fin Lógica de Promociones ---


        // --- Mapear Promociones Asociadas ---
        List<PromocionSimpleDto> promocionesAsociadasDto = Collections.emptyList();
        try {
            Hibernate.initialize(producto.getPromociones());
            Set<Promocion> promocionesAsociadas = producto.getPromociones();
            if (promocionesAsociadas != null && !promocionesAsociadas.isEmpty()) {
                promocionesAsociadasDto = promocionesAsociadas.stream()
                        .map(promo -> PromocionSimpleDto.builder()
                                .idPromocion(promo.getIdPromocion())
                                .codigo(promo.getCodigo())
                                .descripcion(promo.getDescripcion())
                                .descuento(promo.getDescuento())
                                .activa(promo.isActiva())
                                .build())
                        .collect(Collectors.toList());
                log.trace("Mapeadas {} promociones asociadas para Producto ID {}", promocionesAsociadasDto.size(), producto.getIdProducto());
            } else {
                log.trace("Producto ID {} no tiene promociones asociadas.", producto.getIdProducto());
            }
        } catch (Exception e) {
            log.error("Error al inicializar o mapear promociones asociadas para Producto ID {}: {}", producto.getIdProducto(), e.getMessage());
            promocionesAsociadasDto = Collections.emptyList();
        }


        return ProductoResponse.builder()
                .id(producto.getIdProducto())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .talla(producto.getTalla() != null ? producto.getTalla().name() : "N/A")
                .precio(precioConDescuento) // <<< PRECIO FINAL CON DESCUENTO
                .activo(producto.getActivo())
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin Categoría")
                .stock(inventario.getStock())
                .imageUrl(producto.getImageUrl())
                // Nuevos campos
                .precioOriginal(precioOriginal) // <<< PRECIO BASE
                .descuentoAplicado(descuentoAplicado)
                .nombrePromocion(nombrePromocion)
                .promocionesAsociadas(promocionesAsociadasDto)
                .build();
    }


    // --- Resto de los métodos del servicio (sin cambios) ---
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosActivos() { /* ... código existente ... */
        log.info("Obteniendo todos los productos activos.");
        return productoRepository.findByActivoTrue().stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosIncludingInactive() { /* ... código existente ... */
        log.info("Admin: Obteniendo todos los productos (activos e inactivos).");
        return productoRepository.findAll().stream() // Usar findAll() en lugar de findByActivoTrue()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Integer id) { /* ... código existente ... */
        log.info("Obteniendo producto por ID: {}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
        return mapToProductoResponse(producto);
    }
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(String nombreCategoria) { /* ... código existente ... */
        log.info("Obteniendo productos por categoría: {}", nombreCategoria);
        return productoRepository.findByCategoriaNombre(nombreCategoria).stream()
                .filter(Producto::getActivo) // Aseguramos que solo devuelva activos
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) { /* ... código existente ... */
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
    public ProductoResponse updateProducto(Integer id, ProductoRequest request) { /* ... código existente ... */
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
    public void deleteProducto(Integer id) { /* ... código existente ... */
        log.info("Admin: Desactivando (soft delete) producto ID: {}", id);
        // Implementamos Soft Delete (Borrado Lógico)
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Admin: Producto ID {} desactivado.", id);

    }
    @Override
    @Transactional
    public void associatePromocionToProducto(Integer productoId, Integer promocionId) { /* ... código existente ... */
        log.info("Admin: Asociando Promoción ID {} a Producto ID {}", promocionId, productoId);
        // Usar findById para obtener entidades gestionadas
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + promocionId));

        // Añadir la promoción al Set del producto (JPA maneja la tabla intermedia)
        producto.getPromociones().add(promocion);
        // No es necesario guardar explícitamente si la transacción se completa
        // productoRepository.save(producto);
        log.info("Admin: Asociación completada.");
    }
    @Override
    @Transactional
    public void disassociatePromocionFromProducto(Integer productoId, Integer promocionId) { /* ... código existente ... */
        log.info("Admin: Desasociando Promoción ID {} de Producto ID {}", promocionId, productoId);
        // Usar findById para obtener entidades gestionadas
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + promocionId));

        // Quitar la promoción del Set del producto (JPA maneja la tabla intermedia)
        producto.getPromociones().remove(promocion);
        // No es necesario guardar explícitamente si la transacción se completa
        // productoRepository.save(producto);
        log.info("Admin: Desasociación completada.");
    }
}

