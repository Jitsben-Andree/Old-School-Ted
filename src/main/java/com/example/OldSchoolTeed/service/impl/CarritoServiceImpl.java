package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.DetalleCarritoResponse;
import com.example.OldSchoolTeed.dto.ProductoResponse; // Importar ProductoResponse
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.CarritoService;
import com.example.OldSchoolTeed.service.ProductoService; // Importar ProductoService
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; // Importar Valid
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList; // Importar ArrayList
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarritoServiceImpl implements CarritoService {

    private static final Logger log = LoggerFactory.getLogger(CarritoServiceImpl.class); // Añadir logger
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final ProductoService productoService; // Inyectar ProductoService

    public CarritoServiceImpl(CarritoRepository carritoRepository,
                              DetalleCarritoRepository detalleCarritoRepository,
                              UsuarioRepository usuarioRepository,
                              ProductoRepository productoRepository,
                              InventarioRepository inventarioRepository,
                              ProductoService productoService) { // Añadir ProductoService
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.productoService = productoService; // Asignar
    }

    // --- Lógica de Mapeo (Helper) ---
    private CarritoResponse mapToCarritoResponse(Carrito carrito) {
        log.trace("Mapeando Carrito ID: {}", carrito.getIdCarrito());
        // Asegurarse de que la lista no sea null antes de hacer stream
        List<DetalleCarrito> detalles = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : new ArrayList<>();

        List<DetalleCarritoResponse> itemResponses = detalles.stream()
                .map(detalle -> {
                    // Obtener el ProductoResponse (que ya tiene el precio con descuento calculado)
                    // Usamos try-catch por si el producto fue desactivado mientras estaba en el carrito
                    ProductoResponse productoConDescuento;
                    try {
                        productoConDescuento = productoService.getProductoById(detalle.getProducto().getIdProducto());
                    } catch (EntityNotFoundException e) {
                        log.warn("Producto ID {} en DetalleCarrito ID {} no encontrado al mapear. Usando precio 0.",
                                detalle.getProducto().getIdProducto(), detalle.getIdDetalleCarrito());
                        // Crear un DTO temporal si el producto no se encuentra para evitar NullPointerException
                        productoConDescuento = ProductoResponse.builder()
                                .id(detalle.getProducto().getIdProducto())
                                .nombre(detalle.getProducto().getNombre() + " (No disponible)")
                                .precio(BigDecimal.ZERO)
                                .precioOriginal(BigDecimal.ZERO)
                                .build();
                    }

                    BigDecimal precioUnitarioFinal = productoConDescuento.getPrecio(); // Este es el precio con descuento si aplica

                    log.trace("Mapeando DetalleCarrito ID: {}, Producto ID: {}, Cantidad: {}, Precio Final: {}",
                            detalle.getIdDetalleCarrito(), detalle.getProducto().getIdProducto(), detalle.getCantidad(), precioUnitarioFinal);

                    return DetalleCarritoResponse.builder()
                            .detalleCarritoId(detalle.getIdDetalleCarrito())
                            .productoId(detalle.getProducto().getIdProducto())
                            .productoNombre(detalle.getProducto().getNombre())
                            .cantidad(detalle.getCantidad())
                            .precioUnitario(precioUnitarioFinal) // <<< Usar precio con descuento
                            .subtotal(precioUnitarioFinal.multiply(BigDecimal.valueOf(detalle.getCantidad()))) // <<< Calcular subtotal con descuento
                            .imageUrl(detalle.getProducto().getImageUrl()) // Añadir si ya tienes imágenes
                            .build();
                })
                .collect(Collectors.toList());

        // Recalcular el total basado en los subtotales con descuento
        BigDecimal totalConDescuento = itemResponses.stream()
                .map(DetalleCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Total del carrito ID {} calculado con descuentos: {}", carrito.getIdCarrito(), totalConDescuento);


        return CarritoResponse.builder()
                .carritoId(carrito.getIdCarrito())
                .usuarioId(carrito.getUsuario().getIdUsuario())
                .items(itemResponses)
                .total(totalConDescuento) // <<< Usar total con descuento
                .build();
    }

    // --- Lógica de Negocio (Helper) ---
    private Carrito getOrCreateCarrito(Usuario usuario) {
        log.debug("Buscando o creando carrito para usuario ID: {}", usuario.getIdUsuario());
        // Usar FETCH JOIN para cargar detalles eficientemente si se define el método en el repo
        // Optional<Carrito> carritoOpt = carritoRepository.findByUsuarioWithDetails(usuario);
        Optional<Carrito> carritoOpt = carritoRepository.findByUsuario(usuario);


        return carritoOpt.orElseGet(() -> {
            log.info("No se encontró carrito para usuario ID {}, creando uno nuevo.", usuario.getIdUsuario());
            Carrito nuevoCarrito = new Carrito();
            nuevoCarrito.setUsuario(usuario);
            // fechaCreacion se setea con @PrePersist
            nuevoCarrito.setDetallesCarrito(new ArrayList<>()); // Inicializar lista
            return carritoRepository.save(nuevoCarrito);
        });
    }

    @Override
    @Transactional // Permitir escritura para getOrCreateCarrito
    public CarritoResponse getCarritoByUsuario(String userEmail) {
        log.info("Obteniendo carrito para usuario: {}", userEmail);
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = getOrCreateCarrito(usuario);
        // Forzar la carga de detalles si es LAZY (puede ayudar con NullPointerException)
        if(carrito.getDetallesCarrito() != null) carrito.getDetallesCarrito().size();
        return mapToCarritoResponse(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse addItemToCarrito(String userEmail, @Valid AddItemRequest request) { // Añadir @Valid
        log.info("Añadiendo item al carrito para usuario: {}, Producto ID: {}, Cantidad: {}", userEmail, request.getProductoId(), request.getCantidad());

        // 1. Obtener usuario y producto (con validación clara)
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con email: " + userEmail));
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + request.getProductoId()));

        // La validación @Min(1) en AddItemRequest ya cubre cantidad <= 0 si @Valid está presente
        // Pero añadimos un check extra por seguridad
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            log.warn("Intento de añadir cantidad inválida ({}) para Producto ID {} por Usuario {}",
                    request.getCantidad(), request.getProductoId(), userEmail);
            throw new RuntimeException("La cantidad debe ser mayor que cero.");
        }


        // 2. Validar Stock (con validación clara y mensaje específico)
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para el producto ID: " + request.getProductoId()));

        // 3. Obtener o crear carrito
        Carrito carrito = getOrCreateCarrito(usuario);

        // 4. Verificar si el producto ya está en el carrito
        // Asegurarse de que la lista no sea null
        List<DetalleCarrito> detallesActuales = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : new ArrayList<>();
        Optional<DetalleCarrito> itemExistente = detallesActuales.stream()
                .filter(detalle -> detalle.getProducto().getIdProducto().equals(request.getProductoId()))
                .findFirst();

        int cantidadNecesariaTotal;

        if (itemExistente.isPresent()) {
            cantidadNecesariaTotal = itemExistente.get().getCantidad() + request.getCantidad();
            log.debug("Producto ID {} ya existe en carrito ID {}. Cantidad anterior: {}, Nueva cantidad total: {}",
                    request.getProductoId(), carrito.getIdCarrito(), itemExistente.get().getCantidad(), cantidadNecesariaTotal);
        } else {
            cantidadNecesariaTotal = request.getCantidad();
            log.debug("Producto ID {} es nuevo en carrito ID {}. Cantidad a añadir: {}",
                    request.getProductoId(), carrito.getIdCarrito(), cantidadNecesariaTotal);
        }

        // 5. Validar stock ANTES de modificar el carrito
        log.debug("Validando stock. Disponible: {}, Necesario: {}", inventario.getStock(), cantidadNecesariaTotal);
        if (inventario.getStock() < cantidadNecesariaTotal) {
            String errorMsg = "Stock insuficiente para '" + producto.getNombre() + "'. Stock disponible: " + inventario.getStock() + ", necesitas en total: " + cantidadNecesariaTotal;
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        log.debug("Stock validado con éxito.");


        // 6. Ahora sí, modificar el carrito
        if (itemExistente.isPresent()) {
            // Actualizar cantidad si ya existe
            DetalleCarrito detalle = itemExistente.get();
            detalle.setCantidad(cantidadNecesariaTotal);
            detalleCarritoRepository.save(detalle);
            log.info("Cantidad actualizada para DetalleCarrito ID {} a {}", detalle.getIdDetalleCarrito(), cantidadNecesariaTotal);
        } else {
            // Añadir nuevo item si no existe
            DetalleCarrito nuevoDetalle = new DetalleCarrito();
            nuevoDetalle.setCarrito(carrito);
            nuevoDetalle.setProducto(producto);
            nuevoDetalle.setCantidad(request.getCantidad());
            // Guardamos el detalle y lo añadimos a la colección del carrito
            DetalleCarrito detalleGuardado = detalleCarritoRepository.save(nuevoDetalle);
            // Asegurar que la colección esté inicializada antes de añadir
            if (carrito.getDetallesCarrito() == null) {
                carrito.setDetallesCarrito(new ArrayList<>());
            }
            carrito.getDetallesCarrito().add(detalleGuardado); // Añadir a la lista en memoria
            log.info("Nuevo DetalleCarrito creado con ID {} para Carrito ID {}", detalleGuardado.getIdDetalleCarrito(), carrito.getIdCarrito());

        }
        // Guardamos el carrito para asegurar la relación (opcional si cascade está bien)
        carritoRepository.save(carrito);

        // 7. Devolver el carrito mapeado
        // Recargamos el carrito para obtener la lista actualizada desde la BD (más seguro)
        Carrito carritoActualizado = carritoRepository.findById(carrito.getIdCarrito()).get();
        return mapToCarritoResponse(carritoActualizado);
    }

    @Override
    @Transactional
    public CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId) {
        log.info("Eliminando item DetalleCarrito ID {} para usuario {}", detalleCarritoId, userEmail);
        // 1. Obtener usuario y carrito (con validación)
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado.")); // Validar que el carrito exista

        // 2. Encontrar el detalle del carrito (con validación)
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId)
                .orElseThrow(() -> new EntityNotFoundException("Item del carrito no encontrado con ID: " + detalleCarritoId));

        // 3. Validar que el item pertenezca al carrito del usuario
        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            log.error("Acceso denegado: Usuario {} intentó eliminar DetalleCarrito ID {} que no le pertenece (pertenece a Carrito ID {}).",
                    userEmail, detalleCarritoId, detalle.getCarrito().getIdCarrito());
            throw new SecurityException("Acceso denegado: Este item no pertenece a tu carrito.");
        }

        // 4. Eliminar el item
        // Primero remover de la colección en memoria si es necesario y está cargada
        if(carrito.getDetallesCarrito() != null){
            // Importante: Usar equals() para comparar objetos o IDs si es necesario
            carrito.getDetallesCarrito().removeIf(d -> d.getIdDetalleCarrito().equals(detalleCarritoId));
        }
        // Luego eliminar de la base de datos
        detalleCarritoRepository.delete(detalle);
        log.info("DetalleCarrito ID {} eliminado con éxito.", detalleCarritoId);


        // 5. Recargar el carrito para devolverlo actualizado (más seguro)
        Carrito carritoActualizado = carritoRepository.findById(carrito.getIdCarrito()).get();
        return mapToCarritoResponse(carritoActualizado);
    }
}

