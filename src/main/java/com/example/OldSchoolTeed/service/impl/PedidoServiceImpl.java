package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.AdminUpdateEnvioRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePagoRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePedidoStatusRequest;
import com.example.OldSchoolTeed.dto.DetallePedidoResponse;
import com.example.OldSchoolTeed.dto.PedidoRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.entities.Carrito;
import com.example.OldSchoolTeed.entities.DetalleCarrito;
import com.example.OldSchoolTeed.entities.DetallePedido;
import com.example.OldSchoolTeed.entities.Envio;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Pago;
import com.example.OldSchoolTeed.entities.Pedido;
import com.example.OldSchoolTeed.entities.Usuario;
import com.example.OldSchoolTeed.repository.CarritoRepository;
import com.example.OldSchoolTeed.repository.DetalleCarritoRepository;
import com.example.OldSchoolTeed.repository.DetallePedidoRepository;
import com.example.OldSchoolTeed.repository.EnvioRepository;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.PagoRepository;
import com.example.OldSchoolTeed.repository.PedidoRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import com.example.OldSchoolTeed.service.PedidoService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // Asegúrate de importar ArrayList
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class); // Añadir logger
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final InventarioRepository inventarioRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             UsuarioRepository usuarioRepository,
                             CarritoRepository carritoRepository,
                             DetalleCarritoRepository detalleCarritoRepository,
                             InventarioRepository inventarioRepository,
                             PagoRepository pagoRepository,
                             EnvioRepository envioRepository,
                             DetallePedidoRepository detallePedidoRepository,
                             ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.inventarioRepository = inventarioRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
    }


    // --- Métodos de Cliente ---

    @Override
    @Transactional // Quitar readOnly = false (default)
    public PedidoResponse crearPedidoDesdeCarrito(String usuarioEmail, PedidoRequest request) {
        // Log al inicio del método
        log.info("Iniciando creación de pedido para usuario: {}", usuarioEmail);
        try { // Bloque try general para capturar cualquier excepción inesperada durante la creación
            Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                    .orElseThrow(() -> {
                        log.error("Usuario no encontrado al crear pedido: {}", usuarioEmail);
                        return new EntityNotFoundException("Usuario no encontrado");
                    });

            Carrito carrito = carritoRepository.findByUsuario(usuario)
                    .orElseThrow(() -> {
                        log.error("Carrito no encontrado para usuario: {}", usuarioEmail);
                        return new EntityNotFoundException("Carrito no encontrado.");
                    });

            // Asegurarse de que la lista no sea null y cargarla si es LAZY
            List<DetalleCarrito> detallesCarrito = carrito.getDetallesCarrito();
            if (detallesCarrito == null) {
                detallesCarrito = new ArrayList<>(); // Inicializar si es null
                log.warn("La lista detallesCarrito era null para el carrito ID: {}, inicializando.", carrito.getIdCarrito());
            } else {
                // Forzar carga si es LAZY (puede evitar errores posteriores)
                detallesCarrito.size();
                log.debug("Detalles del carrito cargados ({} items) para carrito ID: {}", detallesCarrito.size(), carrito.getIdCarrito());
            }


            if (detallesCarrito.isEmpty()) {
                log.warn("Intento de crear pedido con carrito vacío para usuario: {}", usuarioEmail);
                throw new RuntimeException("El carrito está vacío, no se puede crear un pedido.");
            }

            log.info("Validando stock para {} items del carrito ID: {}", detallesCarrito.size(), carrito.getIdCarrito());
            // 1. Validar Stock ANTES de crear el pedido
            for (DetalleCarrito detalle : detallesCarrito) {
                Integer productoId = detalle.getProducto().getIdProducto();
                String productoNombre = detalle.getProducto().getNombre();
                Integer cantidadRequerida = detalle.getCantidad();
                log.debug("Validando stock para Producto ID: {}, Nombre: {}, Cantidad: {}", productoId, productoNombre, cantidadRequerida);

                Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto())
                        .orElseThrow(() -> {
                            log.error("Inventario no encontrado para Producto ID: {}", productoId);
                            return new EntityNotFoundException("Inventario no encontrado para: " + productoNombre);
                        });

                log.debug("Stock actual para Producto ID {}: {}", productoId, inventario.getStock());
                if (inventario.getStock() < cantidadRequerida) {
                    String errorMsg = "Stock insuficiente. No hay " + cantidadRequerida + " unidades de: " + productoNombre + " (Disponibles: " + inventario.getStock() + ")";
                    log.error("!!! ERROR DE STOCK: {} !!!", errorMsg); // Log de error específico
                    throw new RuntimeException(errorMsg); // Lanzar excepción que causa el 400
                }
            }
            log.info("Validación de stock completada con éxito.");

            // 2. Crear Pedido
            log.info("Creando entidad Pedido...");
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            // fecha y estado se setean con @PrePersist
            // pedido.setFecha(LocalDateTime.now());
            // pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);

            BigDecimal total = detallesCarrito.stream()
                    .map(detalle -> detalle.getProducto().getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            pedido.setTotal(total);
            log.debug("Total del pedido calculado: {}", total);

            Pedido pedidoGuardado = pedidoRepository.save(pedido);
            log.info("Pedido guardado con ID: {}", pedidoGuardado.getIdPedido());

            // 3. Crear Pago y Envío (iniciales)
            log.info("Creando entidades Pago y Envío...");
            Pago.MetodoPago metodoPago;
            // Validar que el método de pago no sea null o vacío antes de convertir
            if (request.getMetodoPagoInfo() == null || request.getMetodoPagoInfo().trim().isEmpty()) {
                log.error("!!! ERROR DE METODO DE PAGO: Método de pago recibido es null o vacío !!!");
                throw new RuntimeException("Debe seleccionar un método de pago.");
            }
            String metodoPagoInfoUpper = request.getMetodoPagoInfo().toUpperCase();
            try {
                metodoPago = Pago.MetodoPago.valueOf(metodoPagoInfoUpper);
                log.debug("Método de pago parseado: {}", metodoPago);
            } catch (IllegalArgumentException e) {
                log.error("!!! ERROR DE METODO DE PAGO: Método inválido recibido: '{}' (intentó buscar '{}') !!!", request.getMetodoPagoInfo(), metodoPagoInfoUpper, e); // Log de error específico
                throw new RuntimeException("Método de pago no válido: " + request.getMetodoPagoInfo()); // Lanzar excepción
            }

            Pago pago = new Pago();
            pago.setPedido(pedidoGuardado);
            pago.setMetodo(metodoPago);
            pago.setMonto(total);
            // fechaPago y estado se setean con @PrePersist
            Pago pagoGuardado = pagoRepository.save(pago);
            log.debug("Pago guardado con ID: {}", pagoGuardado.getIdPago());

            // Validar que la dirección de envío no sea null o vacía
            if (request.getDireccionEnvio() == null || request.getDireccionEnvio().trim().isEmpty()) {
                log.error("!!! ERROR DE DIRECCION: Dirección de envío recibida es null o vacía !!!");
                throw new RuntimeException("Debe ingresar una dirección de envío.");
            }
            Envio envio = new Envio();
            envio.setPedido(pedidoGuardado);
            envio.setDireccionEnvio(request.getDireccionEnvio());
            // fechaEnvio es null y estado se setea con @PrePersist
            Envio envioGuardado = envioRepository.save(envio);
            log.debug("Envío guardado con ID: {}", envioGuardado.getIdEnvio());


            // 4. Mover detalles de Carrito a Pedido y Actualizar Stock
            log.info("Moviendo detalles de Carrito a Pedido y actualizando stock...");
            List<DetallePedido> detallesPedidoGuardados = new ArrayList<>();
            // Crear copia para iterar y eliminar de la original de forma segura
            List<DetalleCarrito> detallesAEliminar = new ArrayList<>(detallesCarrito);

            for (DetalleCarrito detalle : detallesAEliminar) {
                log.debug("Procesando DetalleCarrito ID: {}", detalle.getIdDetalleCarrito());
                // Crear detalle de pedido
                DetallePedido detallePedido = new DetallePedido();
                detallePedido.setPedido(pedidoGuardado);
                detallePedido.setProducto(detalle.getProducto());
                detallePedido.setCantidad(detalle.getCantidad());
                detallePedido.setSubtotal(detalle.getProducto().getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
                detallePedido.setMontoDescuento(BigDecimal.ZERO);
                DetallePedido detallePedidoGuardado = detallePedidoRepository.save(detallePedido);
                detallesPedidoGuardados.add(detallePedidoGuardado);
                log.debug("DetallePedido creado con ID: {}", detallePedidoGuardado.getIdDetallePedido());


                // Actualizar inventario (Reducir stock)
                // Usar .get() es seguro aquí porque ya validamos la existencia del inventario antes
                Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto()).get();
                int stockAnterior = inventario.getStock();
                int nuevoStock = stockAnterior - detalle.getCantidad();
                // Doble check de seguridad (aunque ya validamos antes)
                if (nuevoStock < 0) {
                    log.error("!!! ERROR DE STOCK CRÍTICO: Intento de dejar stock negativo para Producto ID {} !!!", detalle.getProducto().getIdProducto());
                    throw new RuntimeException("Error crítico de stock al actualizar inventario para: " + detalle.getProducto().getNombre());
                }
                inventario.setStock(nuevoStock);
                inventarioRepository.save(inventario);
                log.debug("Stock actualizado para Producto ID {}: {} -> {}", detalle.getProducto().getIdProducto(), stockAnterior, inventario.getStock());


                // Eliminar de detalle_carrito (ahora seguro porque iteramos sobre una copia)
                detalleCarritoRepository.delete(detalle);
                log.debug("DetalleCarrito ID {} eliminado", detalle.getIdDetalleCarrito());

            }
            // Limpiar la lista original del carrito en memoria y persistir el cambio
            carrito.getDetallesCarrito().clear();
            carritoRepository.save(carrito);
            log.info("Todos los detalles movidos y carrito vaciado.");

            // 5. Asociar colecciones al pedido guardado (importante para mapToPedidoResponse)
            pedidoGuardado.setPago(pagoGuardado);
            pedidoGuardado.setEnvio(envioGuardado);
            pedidoGuardado.setDetallesPedido(detallesPedidoGuardados);
            log.info("Asociaciones finales completadas para Pedido ID: {}", pedidoGuardado.getIdPedido());

            // Devolver el pedido mapeado
            return mapToPedidoResponse(pedidoGuardado);

            // --- Bloques Catch Mejorados ---
        } catch (EntityNotFoundException e) {
            // Captura errores específicos de "no encontrado"
            log.error("!!! ERROR - Entidad no encontrada al crear pedido para {}: {} !!!", usuarioEmail, e.getMessage());
            // Lanzar como RuntimeException para que resulte en 404 o 400 según el controlador
            throw new RuntimeException("Error al procesar el pedido: " + e.getMessage());
        } catch (RuntimeException e) {
            // Captura cualquier RuntimeException explícita (Stock, Método Pago, Carrito Vacío, etc.)
            log.error("!!! ERROR de lógica de negocio al crear pedido para {}: {} !!!", usuarioEmail, e.getMessage());
            throw e; // Re-lanzar para que el controlador devuelva 400 con el mensaje específico
        } catch (Exception e) {
            // Captura cualquier otra excepción inesperada (ej. problemas de base de datos)
            log.error("!!! ERROR INESPERADO al crear pedido para {}: {} !!!", usuarioEmail, e.getMessage(), e);
            // Lanzar una genérica para no exponer detalles internos
            throw new RuntimeException("Ocurrió un error inesperado al procesar el pedido. Por favor, intente de nuevo más tarde.");
        }
    }

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

        // Guardar el pedido solo si se modificó su estado o si se asoció un envío nuevo
        if (estadoPedidoModificado) {
            pedidoRepository.save(pedido);
            log.debug("Admin: Entidad Pedido guardada debido a cambios en envío para ID {}", pedidoId);
        }

        // Recargar el pedido para asegurar que la respuesta incluya todas las asociaciones actualizadas
        Pedido pedidoFinal = pedidoRepository.findById(pedidoId).get();
        return mapToPedidoResponse(pedidoFinal);
    }


    // --- Lógica de Mapeo (Helper) ---
    private PedidoResponse mapToPedidoResponse(Pedido pedido) {
        // Asegurarse de que la lista de detalles no sea null y forzar carga si es LAZY
        List<DetallePedido> detalles = pedido.getDetallesPedido();
        if (detalles == null) {
            detalles = new ArrayList<>();
            log.warn("La lista detallesPedido era null para Pedido ID {}, inicializando.", pedido.getIdPedido());
        } else {
            // Forzar carga LAZY
            detalles.size();
        }

        List<DetallePedidoResponse> detallesResponse = detalles.stream()
                .map(detalle -> {
                    // Verificación de producto null en detalle
                    String nombreProducto = (detalle.getProducto() != null) ? detalle.getProducto().getNombre() : "Producto Desconocido";
                    Integer idProducto = (detalle.getProducto() != null) ? detalle.getProducto().getIdProducto() : -1; // Usar -1 o lanzar error si prefieres
                    BigDecimal precioUnitario = (detalle.getProducto() != null) ? detalle.getProducto().getPrecio() : BigDecimal.ZERO;

                    if (detalle.getProducto() == null) {
                        log.warn("DetallePedido ID {} tiene un Producto null.", detalle.getIdDetallePedido());
                    }

                    return DetallePedidoResponse.builder()
                            .detallePedidoId(detalle.getIdDetallePedido())
                            .productoId(idProducto)
                            .productoNombre(nombreProducto)
                            .cantidad(detalle.getCantidad() != null ? detalle.getCantidad() : 0) // Manejar null
                            .precioUnitario(precioUnitario)
                            .subtotal(detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO) // Manejar null
                            .montoDescuento(detalle.getMontoDescuento() != null ? detalle.getMontoDescuento() : BigDecimal.ZERO) // Manejar null
                            .build();
                })
                .collect(Collectors.toList());

        // Obtener info de pago y envío (manejando nulls)
        Pago pago = pedido.getPago();
        Envio envio = pedido.getEnvio();

        // Logging para depurar valores null antes de construir la respuesta
        log.trace("Mapeando Pedido ID: {}. Estado: {}, Total: {}, Pago: {}, Envío: {}",
                pedido.getIdPedido(), pedido.getEstado(), pedido.getTotal(),
                (pago != null ? "ID:" + pago.getIdPago() : "null"),
                (envio != null ? "ID:" + envio.getIdEnvio() : "null"));


        return PedidoResponse.builder()
                .pedidoId(pedido.getIdPedido())
                .fecha(pedido.getFecha())
                // Manejar null para Enums y valores
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : "DESCONOCIDO")
                .total(pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO)
                .detalles(detallesResponse)
                .direccionEnvio(envio != null ? envio.getDireccionEnvio() : "N/A")
                .estadoEnvio(envio != null && envio.getEstado() != null ? envio.getEstado().name() : "N/A")
                .estadoPago(pago != null && pago.getEstado() != null ? pago.getEstado().name() : "N/A")
                .metodoPago(pago != null && pago.getMetodo() != null ? pago.getMetodo().name() : "N/A")
                .build();
    }
}

