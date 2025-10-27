package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.DetalleCarritoResponse;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.dto.UpdateCantidadRequest;
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.CarritoService;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarritoServiceImpl implements CarritoService {

    private static final Logger log = LoggerFactory.getLogger(CarritoServiceImpl.class);
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository; // Asegúrate que esté inyectado
    private final ProductoService productoService;

    // ... (Constructor existente) ...
    public CarritoServiceImpl(CarritoRepository carritoRepository,
                              DetalleCarritoRepository detalleCarritoRepository,
                              UsuarioRepository usuarioRepository,
                              ProductoRepository productoRepository,
                              InventarioRepository inventarioRepository, // Asegurar inyección
                              ProductoService productoService) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository; // Asignar
        this.productoService = productoService;
    }


    // --- Lógica de Mapeo (Helper) ---
    private CarritoResponse mapToCarritoResponse(Carrito carrito) {
        log.trace("Mapeando Carrito ID: {}", carrito.getIdCarrito());
        List<DetalleCarrito> detalles = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : Collections.emptyList(); // Usar Collections.emptyList()

        List<DetalleCarritoResponse> itemResponses = detalles.stream()
                .map(detalle -> {
                    ProductoResponse productoConDescuento;
                    String imageUrl = null;
                    int stockActual = 0; // <<< Variable para guardar el stock

                    try {
                        productoConDescuento = productoService.getProductoById(detalle.getProducto().getIdProducto());
                        imageUrl = productoConDescuento.getImageUrl();
                        // <<< Buscar stock actual del inventario >>>
                        Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto())
                                .orElse(null); // Obtener inventario o null si no existe
                        if (inventario != null) {
                            stockActual = inventario.getStock();
                        } else {
                            log.warn("¡Inventario no encontrado para Producto ID {} al mapear carrito!", detalle.getProducto().getIdProducto());
                        }
                        // <<< Fin búsqueda de stock >>>

                    } catch (EntityNotFoundException e) {
                        log.warn("Producto ID {} en DetalleCarrito ID {} no encontrado al mapear. Usando defaults.",
                                detalle.getProducto().getIdProducto(), detalle.getIdDetalleCarrito());
                        productoConDescuento = ProductoResponse.builder()
                                .id(detalle.getProducto().getIdProducto())
                                .nombre(detalle.getProducto().getNombre() + " (No disponible)")
                                .precio(BigDecimal.ZERO).precioOriginal(BigDecimal.ZERO).build();
                        stockActual = 0; // Stock 0 si el producto no se encuentra
                    }

                    BigDecimal precioUnitarioFinal = productoConDescuento.getPrecio();
                    log.trace("Mapeando DetalleCarrito ID: {}, Producto ID: {}, Cantidad: {}, Precio Final: {}, Stock: {}",
                            detalle.getIdDetalleCarrito(), detalle.getProducto().getIdProducto(), detalle.getCantidad(), precioUnitarioFinal, stockActual);

                    return DetalleCarritoResponse.builder()
                            .detalleCarritoId(detalle.getIdDetalleCarrito())
                            .productoId(detalle.getProducto().getIdProducto())
                            .productoNombre(detalle.getProducto().getNombre())
                            .cantidad(detalle.getCantidad())
                            .precioUnitario(precioUnitarioFinal)
                            .subtotal(precioUnitarioFinal.multiply(BigDecimal.valueOf(detalle.getCantidad())))
                            .imageUrl(imageUrl)
                            .stockActual(stockActual) // <<< Añadir stock al builder
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalConDescuento = itemResponses.stream().map(DetalleCarritoResponse::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Total carrito ID {} con descuentos: {}", carrito.getIdCarrito(), totalConDescuento);

        return CarritoResponse.builder()
                .carritoId(carrito.getIdCarrito())
                .usuarioId(carrito.getUsuario().getIdUsuario())
                .items(itemResponses)
                .total(totalConDescuento)
                .build();
    }

    // ... (Resto de los métodos: getOrCreateCarrito, getCarritoByUsuario, addItemToCarrito, removeItemFromCarrito, updateItemQuantity sin cambios)...
    private Carrito getOrCreateCarrito(Usuario usuario) { /* ... código existente ... */
        log.debug("Buscando o creando carrito para usuario ID: {}", usuario.getIdUsuario());
        Optional<Carrito> carritoOpt = carritoRepository.findByUsuario(usuario);
        return carritoOpt.orElseGet(() -> {
            log.info("Creando nuevo carrito para usuario ID {}.", usuario.getIdUsuario());
            Carrito nuevoCarrito = new Carrito();
            nuevoCarrito.setUsuario(usuario);
            nuevoCarrito.setDetallesCarrito(new ArrayList<>());
            return carritoRepository.save(nuevoCarrito);
        });
    }
    @Override
    @Transactional
    public CarritoResponse getCarritoByUsuario(String userEmail) { /* ... código existente ... */
        log.info("Obteniendo carrito para usuario: {}", userEmail);
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = getOrCreateCarrito(usuario);
        if(carrito.getDetallesCarrito() != null) carrito.getDetallesCarrito().size();
        return mapToCarritoResponse(carrito);
    }
    @Override
    @Transactional
    public CarritoResponse addItemToCarrito(String userEmail, @Valid AddItemRequest request) { /* ... código existente ... */
        log.info("Añadiendo item al carrito para {}: Prod ID {}, Cant {}", userEmail, request.getProductoId(), request.getCantidad());
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + userEmail));
        Producto producto = productoRepository.findById(request.getProductoId()).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + request.getProductoId()));
        if (request.getCantidad() == null || request.getCantidad() <= 0) { throw new RuntimeException("La cantidad debe ser mayor que cero."); }
        Inventario inventario = inventarioRepository.findByProducto(producto).orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para producto ID: " + request.getProductoId()));
        Carrito carrito = getOrCreateCarrito(usuario);
        List<DetalleCarrito> detallesActuales = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : new ArrayList<>();
        Optional<DetalleCarrito> itemExistente = detallesActuales.stream().filter(d -> d.getProducto().getIdProducto().equals(request.getProductoId())).findFirst();
        int cantidadNecesariaTotal = itemExistente.map(detalleCarrito -> detalleCarrito.getCantidad() + request.getCantidad()).orElseGet(request::getCantidad);
        log.debug("Validando stock. Disponible: {}, Necesario: {}", inventario.getStock(), cantidadNecesariaTotal);
        if (inventario.getStock() < cantidadNecesariaTotal) {
            String errorMsg = "Stock insuficiente para '" + producto.getNombre() + "'. Stock: " + inventario.getStock() + ", necesitas: " + cantidadNecesariaTotal;
            log.error(errorMsg); throw new RuntimeException(errorMsg);
        }
        log.debug("Stock OK.");
        if (itemExistente.isPresent()) {
            DetalleCarrito detalle = itemExistente.get();
            detalle.setCantidad(cantidadNecesariaTotal);
            detalleCarritoRepository.save(detalle); log.info("Cantidad actualizada DetalleID {} a {}", detalle.getIdDetalleCarrito(), cantidadNecesariaTotal);
        } else {
            DetalleCarrito nuevoDetalle = new DetalleCarrito();
            nuevoDetalle.setCarrito(carrito); nuevoDetalle.setProducto(producto); nuevoDetalle.setCantidad(request.getCantidad());
            DetalleCarrito detalleGuardado = detalleCarritoRepository.save(nuevoDetalle);
            if (carrito.getDetallesCarrito() == null) carrito.setDetallesCarrito(new ArrayList<>());
            carrito.getDetallesCarrito().add(detalleGuardado); log.info("Nuevo DetalleID {} creado para CarritoID {}", detalleGuardado.getIdDetalleCarrito(), carrito.getIdCarrito());
        }
        carritoRepository.save(carrito);
        Carrito carritoActualizado = carritoRepository.findById(carrito.getIdCarrito()).get();
        return mapToCarritoResponse(carritoActualizado);
    }
    @Override
    @Transactional
    public CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId) { /* ... código existente ... */
        log.info("Eliminando item DetalleCarrito ID {} para usuario {}", detalleCarritoId, userEmail);
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuario(usuario).orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado."));
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId).orElseThrow(() -> new EntityNotFoundException("Item no encontrado ID: " + detalleCarritoId));
        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            log.error("Acceso denegado: Usuario {} intentó eliminar DetalleID {} que no le pertenece (CarritoID {}).", userEmail, detalleCarritoId, detalle.getCarrito().getIdCarrito());
            throw new SecurityException("Acceso denegado: Este item no pertenece a tu carrito.");
        }
        if(carrito.getDetallesCarrito() != null) carrito.getDetallesCarrito().removeIf(d -> d.getIdDetalleCarrito().equals(detalleCarritoId));
        detalleCarritoRepository.delete(detalle);
        log.info("DetalleCarrito ID {} eliminado.", detalleCarritoId);
        Carrito carritoActualizado = carritoRepository.findById(carrito.getIdCarrito()).get();
        return mapToCarritoResponse(carritoActualizado);
    }
    @Override
    @Transactional
    public CarritoResponse updateItemQuantity(String userEmail, Integer detalleCarritoId, @Valid UpdateCantidadRequest request) { /* ... código existente ... */
        log.info("Actualizando cantidad para DetalleCarrito ID {} a {} para usuario {}",
                detalleCarritoId, request.getNuevaCantidad(), userEmail);
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuario(usuario).orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado."));
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId).orElseThrow(() -> new EntityNotFoundException("Item no encontrado ID: " + detalleCarritoId));
        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            log.error("!!! ACCESO DENEGADO al actualizar cantidad: Usuario {} intentó actualizar DetalleCarrito ID {} que no le pertenece (pertenece a Carrito ID {}).", userEmail, detalleCarritoId, detalle.getCarrito().getIdCarrito());
            throw new SecurityException("Acceso denegado: Este item no pertenece a tu carrito.");
        }
        log.debug("Validación de pertenencia OK para DetalleCarrito ID {}", detalleCarritoId);
        Producto producto = detalle.getProducto();
        Inventario inventario = inventarioRepository.findByProducto(producto).orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para producto ID: " + producto.getIdProducto()));
        int nuevaCantidad = request.getNuevaCantidad();
        int stockActual = inventario.getStock();
        log.info(">>> Verificando Stock para actualizar DetalleID {} (Producto '{}'): Stock Actual={}, Cantidad Solicitada={} <<<", detalleCarritoId, producto.getNombre(), stockActual, nuevaCantidad);
        if (stockActual < nuevaCantidad) {
            String errorMsg = "Stock insuficiente para aumentar la cantidad de '" + producto.getNombre() + "'. Stock disponible: " + stockActual + ", Solicitado: " + nuevaCantidad;
            log.error("!!! ERROR DE STOCK al actualizar cantidad: {} !!!", errorMsg);
            throw new RuntimeException(errorMsg);
        }
        log.info(">>> Stock OK para actualizar cantidad. <<<");
        detalle.setCantidad(nuevaCantidad);
        detalleCarritoRepository.save(detalle);
        log.info("Cantidad actualizada con éxito para DetalleCarrito ID {} a {}", detalleCarritoId, nuevaCantidad);
        Carrito carritoActualizado = carritoRepository.findById(carrito.getIdCarrito()).get();
        return mapToCarritoResponse(carritoActualizado);
    }
}

