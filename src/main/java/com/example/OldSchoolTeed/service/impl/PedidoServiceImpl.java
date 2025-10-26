package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.*; // Importar todos los DTOs
import com.example.OldSchoolTeed.entities.*; // Importar todas las Entidades
import com.example.OldSchoolTeed.repository.*; // Importar todos los Repos
import com.example.OldSchoolTeed.service.PedidoService;
import com.example.OldSchoolTeed.service.ProductoService; // << Importar ProductoService
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // << Importar RoundingMode
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final InventarioRepository inventarioRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository; // Aunque no lo usemos directamente aquí, está bien tenerlo
    private final ProductoService productoService; // << Inyectar ProductoService

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             UsuarioRepository usuarioRepository,
                             CarritoRepository carritoRepository,
                             DetalleCarritoRepository detalleCarritoRepository,
                             InventarioRepository inventarioRepository,
                             PagoRepository pagoRepository,
                             EnvioRepository envioRepository,
                             DetallePedidoRepository detallePedidoRepository,
                             ProductoRepository productoRepository,
                             ProductoService productoService) { // << Añadir al constructor
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.inventarioRepository = inventarioRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
        this.productoService = productoService; // << Asignar
    }


    // --- Métodos de Cliente ---

    @Override
    @Transactional
    public PedidoResponse crearPedidoDesdeCarrito(String usuarioEmail, PedidoRequest request) {
        log.info("Iniciando creación de pedido para usuario: {}", usuarioEmail);
        try {
            Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

            Carrito carrito = carritoRepository.findByUsuario(usuario)
                    .orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado."));

            List<DetalleCarrito> detallesCarrito = carrito.getDetallesCarrito();
            if (detallesCarrito == null) detallesCarrito = new ArrayList<>();
            else detallesCarrito.size(); // Forzar carga LAZY

            if (detallesCarrito.isEmpty()) {
                throw new RuntimeException("El carrito está vacío, no se puede crear un pedido.");
            }

            log.info("Validando stock para {} items del carrito ID: {}", detallesCarrito.size(), carrito.getIdCarrito());
            // 1. Validar Stock ANTES de crear el pedido
            for (DetalleCarrito detalle : detallesCarrito) {
                // ... (lógica de validación de stock existente) ...
                Integer productoId = detalle.getProducto().getIdProducto();
                String productoNombre = detalle.getProducto().getNombre();
                Integer cantidadRequerida = detalle.getCantidad();
                Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto())
                        .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para: " + productoNombre));
                if (inventario.getStock() < cantidadRequerida) {
                    String errorMsg = "Stock insuficiente. No hay " + cantidadRequerida + " unidades de: " + productoNombre + " (Disponibles: " + inventario.getStock() + ")";
                    log.error("!!! ERROR DE STOCK: {} !!!", errorMsg);
                    throw new RuntimeException(errorMsg);
                }
            }
            log.info("Validación de stock completada.");

            // --- Calcular Total y Descuentos ---
            BigDecimal totalPedidoConDescuento = BigDecimal.ZERO;
            List<DetallePedidoInfo> detallesParaGuardar = new ArrayList<>(); // Lista temporal para guardar info calculada

            log.info("Calculando precios finales y descuentos para los detalles...");
            for(DetalleCarrito detalleCarrito : detallesCarrito) {
                // Obtener ProductoResponse (ya tiene el precio con descuento)
                ProductoResponse productoDto = productoService.getProductoById(detalleCarrito.getProducto().getIdProducto());
                BigDecimal precioUnitarioFinal = productoDto.getPrecio(); // Precio con descuento
                BigDecimal precioUnitarioOriginal = productoDto.getPrecioOriginal(); // Precio base
                BigDecimal subtotalFinal = precioUnitarioFinal.multiply(BigDecimal.valueOf(detalleCarrito.getCantidad()))
                        .setScale(2, RoundingMode.HALF_UP); // Redondear subtotal

                // Calcular el monto de descuento para este item (precio original - precio final) * cantidad
                BigDecimal montoDescuentoItem = (precioUnitarioOriginal.subtract(precioUnitarioFinal))
                        .multiply(BigDecimal.valueOf(detalleCarrito.getCantidad()))
                        .setScale(2, RoundingMode.HALF_UP); // Redondear descuento

                totalPedidoConDescuento = totalPedidoConDescuento.add(subtotalFinal);

                // Guardar info temporal para crear DetallePedido después
                detallesParaGuardar.add(new DetallePedidoInfo(detalleCarrito.getProducto(), // Pasar Producto
                        detalleCarrito.getCantidad(), // Pasar Cantidad
                        subtotalFinal,
                        montoDescuentoItem));

                log.debug("Detalle Carrito ID {}: Prod ID {}, Cant {}, Precio Orig {}, Precio Final {}, Subtotal {}, Descuento {}",
                        detalleCarrito.getIdDetalleCarrito(), productoDto.getId(), detalleCarrito.getCantidad(),
                        precioUnitarioOriginal, precioUnitarioFinal, subtotalFinal, montoDescuentoItem);
            }
            log.info("Total del pedido calculado con descuentos: {}", totalPedidoConDescuento);
            // --- Fin Cálculo ---


            // 2. Crear Pedido
            log.info("Creando entidad Pedido...");
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setTotal(totalPedidoConDescuento); // <<< Usar total con descuento
            // estado y fecha se setean por @PrePersist

            Pedido pedidoGuardado = pedidoRepository.save(pedido);
            log.info("Pedido guardado con ID: {}", pedidoGuardado.getIdPedido());

            // 3. Crear Pago y Envío (usando el total con descuento)
            log.info("Creando entidades Pago y Envío...");
            // ... (lógica existente para parsear metodoPago y validar dirección) ...
            Pago.MetodoPago metodoPago;
            if (request.getMetodoPagoInfo() == null || request.getMetodoPagoInfo().trim().isEmpty()) { throw new RuntimeException("Debe seleccionar un método de pago."); }
            try { metodoPago = Pago.MetodoPago.valueOf(request.getMetodoPagoInfo().toUpperCase()); }
            catch (IllegalArgumentException e) { throw new RuntimeException("Método de pago no válido: " + request.getMetodoPagoInfo()); }

            if (request.getDireccionEnvio() == null || request.getDireccionEnvio().trim().isEmpty()) { throw new RuntimeException("Debe ingresar una dirección de envío.");}

            Pago pago = new Pago();
            pago.setPedido(pedidoGuardado);
            pago.setMetodo(metodoPago);
            pago.setMonto(totalPedidoConDescuento); // <<< Usar total con descuento
            Pago pagoGuardado = pagoRepository.save(pago);
            log.debug("Pago guardado con ID: {}", pagoGuardado.getIdPago());

            Envio envio = new Envio();
            envio.setPedido(pedidoGuardado);
            envio.setDireccionEnvio(request.getDireccionEnvio());
            Envio envioGuardado = envioRepository.save(envio);
            log.debug("Envío guardado con ID: {}", envioGuardado.getIdEnvio());


            // 4. Crear Detalles de Pedido (usando la info calculada) y Actualizar Stock
            log.info("Creando Detalles de Pedido y actualizando stock...");
            List<DetallePedido> detallesPedidoGuardados = new ArrayList<>();
            List<DetalleCarrito> detallesAEliminar = new ArrayList<>(detallesCarrito);

            for (DetallePedidoInfo info : detallesParaGuardar) {
                // El detalle original del carrito ya no se necesita aquí, usamos la info guardada
                log.debug("Procesando info para Producto ID: {}", info.producto.getIdProducto());

                // Crear detalle de pedido con info calculada
                DetallePedido detallePedido = new DetallePedido();
                detallePedido.setPedido(pedidoGuardado);
                detallePedido.setProducto(info.producto); // Usar Producto de DetallePedidoInfo
                detallePedido.setCantidad(info.cantidad); // Usar Cantidad de DetallePedidoInfo
                detallePedido.setSubtotal(info.subtotalConDescuento);   // <<< Subtotal con descuento
                detallePedido.setMontoDescuento(info.montoDescuento); // <<< Monto de descuento
                DetallePedido detallePedidoGuardado = detallePedidoRepository.save(detallePedido);
                detallesPedidoGuardados.add(detallePedidoGuardado);
                log.debug("DetallePedido creado ID: {}, Subtotal: {}, Descuento: {}",
                        detallePedidoGuardado.getIdDetallePedido(), info.subtotalConDescuento, info.montoDescuento);

                // Actualizar inventario (igual que antes)
                Inventario inventario = inventarioRepository.findByProducto(info.producto).get();
                int stockAnterior = inventario.getStock();
                int nuevoStock = stockAnterior - info.cantidad; // Usar cantidad de DetallePedidoInfo
                if (nuevoStock < 0) { throw new RuntimeException("Error crítico de stock al actualizar inventario."); } // Seguridad extra
                inventario.setStock(nuevoStock);
                inventarioRepository.save(inventario);
                log.debug("Stock actualizado para Producto ID {}: {} -> {}", info.producto.getIdProducto(), stockAnterior, inventario.getStock());
            }

            // Eliminar todos los detalles del carrito original de una vez
            log.debug("Eliminando {} detalles del Carrito ID {}", detallesAEliminar.size(), carrito.getIdCarrito());
            detalleCarritoRepository.deleteAll(detallesAEliminar);


            // Limpiar la lista original del carrito en memoria y persistir el cambio
            carrito.getDetallesCarrito().clear();
            carritoRepository.save(carrito);
            log.info("Todos los detalles movidos y carrito vaciado.");

            // 5. Asociar colecciones y devolver
            pedidoGuardado.setPago(pagoGuardado);
            pedidoGuardado.setEnvio(envioGuardado);
            pedidoGuardado.setDetallesPedido(detallesPedidoGuardados);
            log.info("Asociaciones finales completadas para Pedido ID: {}", pedidoGuardado.getIdPedido());

            return mapToPedidoResponse(pedidoGuardado);

        } catch (RuntimeException e) {
            log.error("!!! ERROR al crear pedido para {}: {} !!!", usuarioEmail, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("!!! ERROR INESPERADO al crear pedido para {}: {} !!!", usuarioEmail, e.getMessage(), e);
            throw new RuntimeException("Ocurrió un error inesperado al procesar el pedido.");
        }
    }

    // Clase helper interna para guardar info calculada de detalles
    // Modificada para almacenar Producto y Cantidad directamente
    private static class DetallePedidoInfo {
        Producto producto; // Almacenar el objeto Producto
        Integer cantidad;  // Almacenar la cantidad
        BigDecimal subtotalConDescuento;
        BigDecimal montoDescuento;

        DetallePedidoInfo(Producto producto, Integer cantidad, BigDecimal subtotalConDescuento, BigDecimal montoDescuento) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.subtotalConDescuento = subtotalConDescuento;
            this.montoDescuento = montoDescuento;
        }
    }


    // --- El resto de los métodos (getPedidosByUsuario, getPedidoById, Admin, mapToPedidoResponse) ---

    // Asegurarse de que mapToPedidoResponse devuelva el precio unitario original
    private PedidoResponse mapToPedidoResponse(Pedido pedido) {
        // ... (resto del mapeo) ...
        List<DetallePedido> detalles = pedido.getDetallesPedido();
        if (detalles == null) detalles = new ArrayList<>();
        else detalles.size(); // Forzar carga LAZY

        List<DetallePedidoResponse> detallesResponse = detalles.stream()
                .map(detalle -> {
                    BigDecimal precioOriginalUnitario = (detalle.getProducto() != null) ? detalle.getProducto().getPrecio() : BigDecimal.ZERO;

                    return DetallePedidoResponse.builder()
                            // ... (otros campos) ...
                            .detallePedidoId(detalle.getIdDetallePedido())
                            .productoId(detalle.getProducto() != null ? detalle.getProducto().getIdProducto() : -1)
                            .productoNombre(detalle.getProducto() != null ? detalle.getProducto().getNombre() : "Producto Desconocido")
                            .cantidad(detalle.getCantidad() != null ? detalle.getCantidad() : 0)
                            .precioUnitario(precioOriginalUnitario) // <<< Devolver el precio original unitario aquí
                            .subtotal(detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO) // <<< Este es el subtotal CON descuento
                            .montoDescuento(detalle.getMontoDescuento() != null ? detalle.getMontoDescuento() : BigDecimal.ZERO) // Manejar null
                            .build();
                })
                .collect(Collectors.toList());

        // ... (resto del mapeo) ...
        Pago pago = pedido.getPago();
        Envio envio = pedido.getEnvio();
        return PedidoResponse.builder()
                .pedidoId(pedido.getIdPedido())
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : "DESCONOCIDO")
                .total(pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO) // Este es el total CON descuento
                .detalles(detallesResponse)
                .direccionEnvio(envio != null ? envio.getDireccionEnvio() : "N/A")
                .estadoEnvio(envio != null && envio.getEstado() != null ? envio.getEstado().name() : "N/A")
                .estadoPago(pago != null && pago.getEstado() != null ? pago.getEstado().name() : "N/A")
                .metodoPago(pago != null && pago.getMetodo() != null ? pago.getMetodo().name() : "N/A")
                .build();
    }
    // --- Resto de métodos de Admin ---
    // ... (getAllPedidosAdmin, updatePedidoStatusAdmin, etc.) ...
    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> getPedidosByUsuario(String usuarioEmail) {
        log.debug("Buscando pedidos para usuario: {}", usuarioEmail);
        Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        List<Pedido> pedidos = pedidoRepository.findByUsuario(usuario);
        log.info("Encontrados {} pedidos para usuario {}", pedidos.size(), usuarioEmail);
        return pedidos.stream()
                .map(this::mapToPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse getPedidoById(String usuarioEmail, Integer pedidoId) {
        log.debug("Buscando pedido ID {} para usuario {}", pedidoId, usuarioEmail);
        Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        // Validar pertenencia
        if (!pedido.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            log.warn("Acceso denegado: Usuario {} intentó acceder al pedido ID {} que pertenece a otro usuario.", usuarioEmail, pedidoId);
            throw new SecurityException("Acceso denegado: Este pedido no te pertenece.");
        }
        log.info("Pedido ID {} obtenido con éxito para usuario {}", pedidoId, usuarioEmail);
        return mapToPedidoResponse(pedido);
    }


    // --- Métodos de Administrador ---

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> getAllPedidosAdmin() {
        log.info("Admin: Obteniendo todos los pedidos.");
        List<Pedido> pedidos = pedidoRepository.findAll();
        log.info("Admin: {} pedidos encontrados.", pedidos.size());
        return pedidos.stream()
                .map(this::mapToPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PedidoResponse updatePedidoStatusAdmin(Integer pedidoId, AdminUpdatePedidoStatusRequest request) {
        log.info("Admin: Actualizando estado del pedido ID {} a {}", pedidoId, request.getNuevoEstado());
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        try {
            // Conversión segura a mayúsculas
            Pedido.EstadoPedido nuevoEstado = Pedido.EstadoPedido.valueOf(request.getNuevoEstado().toUpperCase());
            pedido.setEstado(nuevoEstado);
            Pedido pedidoActualizado = pedidoRepository.save(pedido);
            log.info("Admin: Estado del pedido ID {} actualizado a {}", pedidoId, nuevoEstado);
            return mapToPedidoResponse(pedidoActualizado);
        } catch (IllegalArgumentException e) {
            log.error("Admin: Estado de pedido inválido recibido: '{}'", request.getNuevoEstado(), e);
            throw new IllegalArgumentException("Estado de pedido no válido: " + request.getNuevoEstado());
        }
    }

    @Override
    @Transactional
    public PedidoResponse updatePagoStatusAdmin(Integer pedidoId, AdminUpdatePagoRequest request) {
        log.info("Admin: Actualizando estado de pago del pedido ID {} a {}", pedidoId, request.getNuevoEstadoPago());
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        Pago pago = pedido.getPago();
        boolean crearPagoNuevo = false;
        if (pago == null) {
            log.warn("Admin: No se encontró pago para pedido ID {}, creando uno nuevo.", pedidoId);
            pago = new Pago();
            pago.setPedido(pedido);
            pago.setMonto(pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO);
            // Default a TARJETA si no existe info previa
            pago.setMetodo(Pago.MetodoPago.TARJETA);
            crearPagoNuevo = true;
        }

        try {
            // Conversión segura a mayúsculas
            Pago.EstadoPago nuevoEstadoPago = Pago.EstadoPago.valueOf(request.getNuevoEstadoPago().toUpperCase());
            pago.setEstado(nuevoEstadoPago);
            if (nuevoEstadoPago == Pago.EstadoPago.COMPLETADO && pago.getFechaPago() == null) {
                pago.setFechaPago(LocalDateTime.now()); // Registrar fecha al completar
                log.debug("Admin: Registrando fecha de pago para pedido ID {}", pedidoId);
            }
            Pago pagoGuardado = pagoRepository.save(pago);
            // Si creamos un pago nuevo, asegurarnos de asociarlo al pedido
            if(crearPagoNuevo) {
                pedido.setPago(pagoGuardado);
            }
            log.info("Admin: Estado de pago del pedido ID {} actualizado a {}", pedidoId, nuevoEstadoPago);


            // Actualizar estado del pedido si el pago se completa
            boolean pedidoModificado = crearPagoNuevo; // Marcar como modificado si creamos pago
            if (nuevoEstadoPago == Pago.EstadoPago.COMPLETADO && pedido.getEstado() == Pedido.EstadoPedido.PENDIENTE) {
                pedido.setEstado(Pedido.EstadoPedido.PAGADO);
                log.info("Admin: Estado del pedido ID {} actualizado a PAGADO debido a pago completado.", pedidoId);
                pedidoModificado = true;
            }
            // Guardar el pedido solo si se modificó su estado o si se creó un pago nuevo
            if (pedidoModificado) {
                pedidoRepository.save(pedido);
            }

            // Recargar el pedido para asegurar que la respuesta incluya todas las asociaciones actualizadas
            Pedido pedidoFinal = pedidoRepository.findById(pedidoId).get();
            return mapToPedidoResponse(pedidoFinal);
        } catch (IllegalArgumentException e) {
            log.error("Admin: Estado de pago inválido recibido: '{}'", request.getNuevoEstadoPago(), e);
            throw new IllegalArgumentException("Estado de pago no válido: " + request.getNuevoEstadoPago());
        }
    }

    @Override
    @Transactional
    public PedidoResponse updateEnvioDetailsAdmin(Integer pedidoId, AdminUpdateEnvioRequest request) {
        log.info("Admin: Actualizando detalles de envío del pedido ID {}", pedidoId);
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        Envio envio = pedido.getEnvio();
        boolean crearEnvioNuevo = false;
        if (envio == null) {
            log.warn("Admin: No se encontró envío para pedido ID {}, creando uno nuevo.", pedidoId);
            envio = new Envio();
            envio.setPedido(pedido);
            // Inicializar con estado EN_PREPARACION si es nuevo
            envio.setEstado(Envio.EstadoEnvio.EN_PREPARACION);
            crearEnvioNuevo = true;
        }

        // Actualizar campos del Envío si vienen en el request
        boolean envioModificado = false;
        if (request.getDireccionEnvio() != null && !request.getDireccionEnvio().trim().isEmpty()) {
            envio.setDireccionEnvio(request.getDireccionEnvio());
            log.debug("Admin: Dirección de envío actualizada para pedido ID {}", pedidoId);
            envioModificado = true;
        }
        if (request.getFechaEnvio() != null) {
            envio.setFechaEnvio(request.getFechaEnvio());
            log.debug("Admin: Fecha de envío actualizada para pedido ID {}", pedidoId);
            envioModificado = true;
        }
        // Añadir lógica para código de seguimiento si lo incluyes en el DTO
        // if (request.getCodigoSeguimiento() != null) {
        //    envio.setCodigoSeguimiento(request.getCodigoSeguimiento());
        //    envioModificado = true;
        // }


        // Actualizar estado del Envío si viene en el request
        boolean estadoPedidoModificado = false;
        if (request.getNuevoEstadoEnvio() != null && !request.getNuevoEstadoEnvio().trim().isEmpty()) {
            try {
                // Conversión segura a mayúsculas
                Envio.EstadoEnvio nuevoEstadoEnvio = Envio.EstadoEnvio.valueOf(request.getNuevoEstadoEnvio().toUpperCase());
                // Evitar actualizar si el estado es el mismo
                if (envio.getEstado() != nuevoEstadoEnvio) {
                    envio.setEstado(nuevoEstadoEnvio);
                    log.info("Admin: Estado de envío del pedido ID {} actualizado a {}", pedidoId, nuevoEstadoEnvio);
                    envioModificado = true;

                    // Lógica de negocio extra: Actualizar Pedido según el Envío
                    if (nuevoEstadoEnvio == Envio.EstadoEnvio.ENTREGADO) {
                        pedido.setEstado(Pedido.EstadoPedido.ENTREGADO);
                        estadoPedidoModificado = true;
                        log.info("Admin: Estado del pedido ID {} actualizado a ENTREGADO.", pedidoId);
                    } else if (nuevoEstadoEnvio == Envio.EstadoEnvio.EN_CAMINO) {
                        // Solo actualizar a ENVIADO si el pedido ya estaba PAGADO o ENVIADO previamente
                        if (pedido.getEstado() == Pedido.EstadoPedido.PAGADO || pedido.getEstado() == Pedido.EstadoPedido.ENVIADO) {
                            pedido.setEstado(Pedido.EstadoPedido.ENVIADO);
                            estadoPedidoModificado = true;
                            log.info("Admin: Estado del pedido ID {} actualizado a ENVIADO.", pedidoId);
                        }
                        // Registrar fecha de envío si se marca "En Camino" y no tiene fecha aún
                        if (envio.getFechaEnvio() == null) {
                            envio.setFechaEnvio(LocalDate.now());
                            log.debug("Admin: Registrando fecha de envío actual para pedido ID {}", pedidoId);
                        }
                    } else if (nuevoEstadoEnvio == Envio.EstadoEnvio.EN_PREPARACION && crearEnvioNuevo) {
                        // Si es un envío nuevo y el estado es EN_PREPARACION, no cambiar estado del pedido
                    }
                }

            } catch (IllegalArgumentException e) {
                log.error("Admin: Estado de envío inválido recibido: '{}'", request.getNuevoEstadoEnvio(), e);
                throw new IllegalArgumentException("Estado de envío no válido: "+ request.getNuevoEstadoEnvio());
            }
        }

        // Guardar el envío solo si se modificó o es nuevo
        if (envioModificado || crearEnvioNuevo) {
            Envio envioGuardado = envioRepository.save(envio);
            // Si creamos uno nuevo, asegurarnos de asociarlo al pedido
            if(crearEnvioNuevo) {
                pedido.setEnvio(envioGuardado);
                // Marcar para guardar pedido si asociamos un nuevo envío
                estadoPedidoModificado = true;
            }
            log.debug("Admin: Entidad Envío guardada para pedido ID {}", pedidoId);
        }

        // Guardar el pedido solo si se modificó su estado o se asoció un envío nuevo
        if (estadoPedidoModificado) {
            pedidoRepository.save(pedido);
            log.debug("Admin: Entidad Pedido guardada debido a cambios en envío para ID {}", pedidoId);
        }

        // Recargar el pedido para asegurar que la respuesta incluya todas las asociaciones actualizadas
        Pedido pedidoFinal = pedidoRepository.findById(pedidoId).get();
        return mapToPedidoResponse(pedidoFinal);
    }
}

