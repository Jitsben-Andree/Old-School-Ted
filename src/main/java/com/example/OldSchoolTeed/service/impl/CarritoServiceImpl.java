package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.DetalleCarritoResponse;
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.CarritoService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger; // Añadir Logger
import org.slf4j.LoggerFactory; // Añadir LoggerFactory
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarritoServiceImpl implements CarritoService {

    // Logger para depuración
    private static final Logger log = LoggerFactory.getLogger(CarritoServiceImpl.class);

    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;

    public CarritoServiceImpl(CarritoRepository carritoRepository,
                              DetalleCarritoRepository detalleCarritoRepository,
                              UsuarioRepository usuarioRepository,
                              ProductoRepository productoRepository,
                              InventarioRepository inventarioRepository) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    // --- Lógica de Mapeo (Helper) ---
    private CarritoResponse mapToCarritoResponse(Carrito carrito) {
        // Asegurarse de que la lista no sea null antes de hacer stream
        List<DetalleCarrito> detalles = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : new ArrayList<>();
        log.debug("Mapeando carrito ID: {}, Detalles encontrados: {}", carrito.getIdCarrito(), detalles.size());


        List<DetalleCarritoResponse> itemResponses = detalles.stream()
                .map(detalle -> {
                    // Log para cada detalle
                    log.trace("Mapeando detalle ID: {}, Producto ID: {}, Cantidad: {}",
                            detalle.getIdDetalleCarrito(), detalle.getProducto().getIdProducto(), detalle.getCantidad());
                    return DetalleCarritoResponse.builder()
                            .detalleCarritoId(detalle.getIdDetalleCarrito())
                            .productoId(detalle.getProducto().getIdProducto())
                            .productoNombre(detalle.getProducto().getNombre())
                            .cantidad(detalle.getCantidad())
                            .precioUnitario(detalle.getProducto().getPrecio())
                            .subtotal(detalle.getProducto().getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                            .imageUrl(detalle.getProducto().getImageUrl()) // Añadido para mostrar imagen
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(DetalleCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total calculado para carrito ID {}: {}", carrito.getIdCarrito(), total);


        return CarritoResponse.builder()
                .carritoId(carrito.getIdCarrito())
                .usuarioId(carrito.getUsuario().getIdUsuario())
                .items(itemResponses)
                .total(total)
                .build();
    }

    // --- Lógica de Negocio (Helper) ---
    private Carrito getOrCreateCarrito(Usuario usuario) {
        log.debug("Buscando o creando carrito para usuario ID: {}", usuario.getIdUsuario());
        // Usar el método optimizado con FETCH JOIN para traer detalles
        return carritoRepository.findByUsuarioWithDetails(usuario)
                .orElseGet(() -> {
                    log.info("Carrito no encontrado para usuario ID: {}. Creando uno nuevo.", usuario.getIdUsuario());
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(usuario);
                    nuevoCarrito.setFechaCreacion(LocalDateTime.now());
                    nuevoCarrito.setDetallesCarrito(new ArrayList<>()); // Inicializar lista
                    Carrito carritoGuardado = carritoRepository.save(nuevoCarrito);
                    log.info("Nuevo carrito creado con ID: {}", carritoGuardado.getIdCarrito());
                    return carritoGuardado;
                });
    }

    @Override
    // Quitar readOnly = true para permitir la creación del carrito
    @Transactional
    public CarritoResponse getCarritoByUsuario(String userEmail) {
        log.info("Obteniendo carrito para usuario: {}", userEmail);
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con email: {}", userEmail);
                    return new EntityNotFoundException("Usuario no encontrado");
                });
        Carrito carrito = getOrCreateCarrito(usuario);
        return mapToCarritoResponse(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse addItemToCarrito(String userEmail, AddItemRequest request) {
        log.info("Intentando añadir item al carrito para usuario: {}, Producto ID: {}, Cantidad: {}",
                userEmail, request.getProductoId(), request.getCantidad());

        // 1. Obtener usuario y producto (con validación clara)
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con email: {}", userEmail);
                    return new EntityNotFoundException("Usuario no encontrado con email: " + userEmail);
                });
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> {
                    log.error("Producto no encontrado con ID: {}", request.getProductoId());
                    return new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductoId());
                });

        // Validar que la cantidad sea positiva
        if (request.getCantidad() <= 0) {
            log.warn("Intento de añadir cantidad inválida ({} <= 0) para producto ID: {}", request.getCantidad(), request.getProductoId());
            throw new RuntimeException("La cantidad debe ser mayor que cero.");
        }

        // 2. Validar Stock (con validación clara y mensaje específico)
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseThrow(() -> {
                    log.error("Inventario no encontrado para producto ID: {}", request.getProductoId());
                    return new EntityNotFoundException("Inventario no encontrado para el producto ID: " + request.getProductoId());
                });
        log.debug("Stock actual para producto ID {}: {}", request.getProductoId(), inventario.getStock());

        // 3. Obtener o crear carrito (usando FETCH JOIN)
        Carrito carrito = getOrCreateCarrito(usuario);

        // 4. Verificar si el producto ya está en el carrito
        Optional<DetalleCarrito> itemExistente = carrito.getDetallesCarrito().stream()
                .filter(detalle -> detalle.getProducto().getIdProducto().equals(request.getProductoId()))
                .findFirst();

        int cantidadNecesariaTotal;

        if (itemExistente.isPresent()) {
            cantidadNecesariaTotal = itemExistente.get().getCantidad() + request.getCantidad();
            log.debug("Producto ID {} ya existe en carrito ID {}. Cantidad actual: {}, Cantidad a añadir: {}, Cantidad total necesaria: {}",
                    request.getProductoId(), carrito.getIdCarrito(), itemExistente.get().getCantidad(), request.getCantidad(), cantidadNecesariaTotal);
        } else {
            cantidadNecesariaTotal = request.getCantidad();
            log.debug("Producto ID {} es nuevo en carrito ID {}. Cantidad total necesaria: {}",
                    request.getProductoId(), carrito.getIdCarrito(), cantidadNecesariaTotal);
        }

        // 5. Validar stock ANTES de modificar el carrito
        if (inventario.getStock() < cantidadNecesariaTotal) {
            log.warn("Stock insuficiente para producto ID {}. Stock: {}, Necesario: {}",
                    request.getProductoId(), inventario.getStock(), cantidadNecesariaTotal);
            throw new RuntimeException("Stock insuficiente para '" + producto.getNombre() + "'. Stock disponible: " + inventario.getStock() + ", necesitas: " + cantidadNecesariaTotal);
        }
        log.debug("Stock suficiente para producto ID {}. Stock: {}, Necesario: {}",
                request.getProductoId(), inventario.getStock(), cantidadNecesariaTotal);


        // 6. Ahora sí, modificar el carrito
        if (itemExistente.isPresent()) {
            // Actualizar cantidad si ya existe
            DetalleCarrito detalle = itemExistente.get();
            detalle.setCantidad(cantidadNecesariaTotal);
            detalleCarritoRepository.save(detalle);
            log.info("Cantidad actualizada para detalle ID {} a {}", detalle.getIdDetalleCarrito(), cantidadNecesariaTotal);
        } else {
            // Añadir nuevo item si no existe
            DetalleCarrito nuevoDetalle = new DetalleCarrito();
            nuevoDetalle.setCarrito(carrito);
            nuevoDetalle.setProducto(producto);
            nuevoDetalle.setCantidad(request.getCantidad());
            // Guardamos el detalle y lo añadimos a la colección del carrito
            DetalleCarrito detalleGuardado = detalleCarritoRepository.save(nuevoDetalle);
            carrito.getDetallesCarrito().add(detalleGuardado); // Añadir a la lista en memoria
            log.info("Nuevo detalle creado con ID {} para carrito ID {}", detalleGuardado.getIdDetalleCarrito(), carrito.getIdCarrito());
        }

        // 7. Devolver el carrito mapeado
        // Recargamos el carrito usando el método con FETCH JOIN para asegurar datos actualizados
        Carrito carritoActualizado = carritoRepository.findByUsuarioWithDetails(usuario).get();
        log.info("Item añadido/actualizado correctamente al carrito ID {}", carritoActualizado.getIdCarrito());
        return mapToCarritoResponse(carritoActualizado);
    }

    @Override
    @Transactional
    public CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId) {
        log.info("Intentando eliminar item detalle ID {} del carrito para usuario: {}", detalleCarritoId, userEmail);
        // 1. Obtener usuario y carrito (con validación y FETCH JOIN)
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado con email: {}", userEmail);
                    return new EntityNotFoundException("Usuario no encontrado");
                });
        Carrito carrito = carritoRepository.findByUsuarioWithDetails(usuario)
                .orElseThrow(() -> {
                    log.warn("Intento de eliminar item, pero el carrito no fue encontrado para usuario ID: {}", usuario.getIdUsuario());
                    return new EntityNotFoundException("Carrito no encontrado.");
                });

        // 2. Encontrar el detalle del carrito (con validación)
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId)
                .orElseThrow(() -> {
                    log.error("Item del carrito no encontrado con ID: {}", detalleCarritoId);
                    return new EntityNotFoundException("Item del carrito no encontrado con ID: " + detalleCarritoId);
                });

        // 3. Validar que el item pertenezca al carrito del usuario
        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            log.error("Intento de eliminar item detalle ID {} que no pertenece al carrito ID {} del usuario ID {}",
                    detalleCarritoId, carrito.getIdCarrito(), usuario.getIdUsuario());
            throw new SecurityException("Acceso denegado: Este item no pertenece a tu carrito.");
        }

        // 4. Eliminar el item
        // Primero remover de la colección en memoria
        boolean removed = carrito.getDetallesCarrito().remove(detalle);
        log.debug("Item detalle ID {} removido de la colección en memoria: {}", detalleCarritoId, removed);
        // Luego eliminar de la base de datos
        detalleCarritoRepository.delete(detalle);
        log.info("Item detalle ID {} eliminado de la base de datos.", detalleCarritoId);


        // 5. Recargar el carrito para devolverlo actualizado (más seguro)
        Carrito carritoActualizado = carritoRepository.findByUsuarioWithDetails(usuario).get();
        log.info("Item eliminado correctamente del carrito ID {}", carritoActualizado.getIdCarrito());
        return mapToCarritoResponse(carritoActualizado);
    }
}

