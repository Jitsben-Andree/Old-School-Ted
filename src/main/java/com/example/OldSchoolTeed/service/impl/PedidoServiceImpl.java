package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.*;
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.PedidoService;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ProductoRepository productoRepository;
    private final ProductoService productoService;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             UsuarioRepository usuarioRepository,
                             CarritoRepository carritoRepository,
                             DetalleCarritoRepository detalleCarritoRepository,
                             InventarioRepository inventarioRepository,
                             PagoRepository pagoRepository,
                             EnvioRepository envioRepository,
                             DetallePedidoRepository detallePedidoRepository,
                             ProductoRepository productoRepository,
                             ProductoService productoService) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.inventarioRepository = inventarioRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.productoRepository = productoRepository;
        this.productoService = productoService;
    }


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

            if (CollectionUtils.isEmpty(detallesCarrito)) {
                log.warn("Intento de crear pedido con carrito vacío para usuario: {}", usuarioEmail);
                throw new RuntimeException("El carrito está vacío, no se puede crear un pedido.");
            } else {
                detallesCarrito.size();
                log.debug("Detalles del carrito cargados ({} items)", detallesCarrito.size());
            }

            // Validar Stock
            log.info("Validando stock para {} items...", detallesCarrito.size());
            for (DetalleCarrito detalle : detallesCarrito) {
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

            // Calcular Total y Descuentos
            BigDecimal totalPedidoConDescuento = BigDecimal.ZERO;
            List<DetallePedidoInfo> detallesParaGuardar = new ArrayList<>();
            log.info("Calculando precios finales y descuentos...");
            for(DetalleCarrito detalleCarrito : detallesCarrito) {
                ProductoResponse productoDto = productoService.getProductoById(detalleCarrito.getProducto().getIdProducto());
                BigDecimal precioUnitarioFinal = productoDto.getPrecio();
                BigDecimal precioUnitarioOriginal = productoDto.getPrecioOriginal() != null ? productoDto.getPrecioOriginal() : precioUnitarioFinal;
                BigDecimal subtotalFinal = precioUnitarioFinal.multiply(BigDecimal.valueOf(detalleCarrito.getCantidad())).setScale(2, RoundingMode.HALF_UP);
                BigDecimal montoDescuentoItem = (precioUnitarioOriginal.subtract(precioUnitarioFinal))
                        .multiply(BigDecimal.valueOf(detalleCarrito.getCantidad()))
                        .setScale(2, RoundingMode.HALF_UP);
                totalPedidoConDescuento = totalPedidoConDescuento.add(subtotalFinal);
                detallesParaGuardar.add(new DetallePedidoInfo(detalleCarrito.getProducto(), detalleCarrito.getCantidad(), subtotalFinal, montoDescuentoItem));
            }
            log.info("Total del pedido calculado con descuentos: {}", totalPedidoConDescuento);


            //  Crear Pedido
            log.info("Creando entidad Pedido...");
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setTotal(totalPedidoConDescuento);
            Pedido pedidoGuardado = pedidoRepository.save(pedido);
            log.info("Pedido guardado con ID: {}", pedidoGuardado.getIdPedido());

            // Crear Pago y Envío
            log.info("Creando entidades Pago y Envío...");
            Pago.MetodoPago metodoPago;

            if (StringUtils.isBlank(request.getMetodoPagoInfo())) {
                log.error("!!! ERROR DE METODO DE PAGO: Método de pago recibido es null o vacío !!!");
                throw new RuntimeException("Debe seleccionar un método de pago.");
            }
            try {
                metodoPago = Pago.MetodoPago.valueOf(request.getMetodoPagoInfo().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("!!! ERROR DE METODO DE PAGO: Método inválido: '{}' !!!", request.getMetodoPagoInfo(), e);
                throw new RuntimeException("Método de pago no válido: " + request.getMetodoPagoInfo());
            }

            Pago pago = new Pago();
            pago.setPedido(pedidoGuardado);
            pago.setMetodo(metodoPago);
            pago.setMonto(totalPedidoConDescuento);
            Pago pagoGuardado = pagoRepository.save(pago);
            log.debug("Pago guardado con ID: {}", pagoGuardado.getIdPago());

            if (StringUtils.isBlank(request.getDireccionEnvio())) {
                log.error("!!! ERROR DE DIRECCION: Dirección de envío recibida es null o vacía !!!");
                throw new RuntimeException("Debe ingresar una dirección de envío.");
            }
            Envio envio = new Envio();
            envio.setPedido(pedidoGuardado);
            envio.setDireccionEnvio(request.getDireccionEnvio());
            Envio envioGuardado = envioRepository.save(envio);
            log.debug("Envío guardado con ID: {}", envioGuardado.getIdEnvio());


            // Crear Detalles de Pedido y Actualizar Stock
            log.info("Creando Detalles de Pedido y actualizando stock...");
            List<DetallePedido> detallesPedidoGuardados = new ArrayList<>();
            List<DetalleCarrito> detallesAEliminar = new ArrayList<>(detallesCarrito);

            for (DetallePedidoInfo info : detallesParaGuardar) {
                DetallePedido detallePedido = new DetallePedido();
                detallePedido.setPedido(pedidoGuardado);
                detallePedido.setProducto(info.producto);
                detallePedido.setCantidad(info.cantidad);
                detallePedido.setSubtotal(info.subtotalConDescuento);
                detallePedido.setMontoDescuento(info.montoDescuento);
                DetallePedido detallePedidoGuardado = detallePedidoRepository.save(detallePedido);
                detallesPedidoGuardados.add(detallePedidoGuardado);

                Inventario inventario = inventarioRepository.findByProducto(info.producto).get();
                int stockAnterior = inventario.getStock();
                int nuevoStock = stockAnterior - info.cantidad;
                if (nuevoStock < 0) { throw new RuntimeException("Error crítico de stock al actualizar inventario."); }
                inventario.setStock(nuevoStock);
                inventarioRepository.save(inventario);
            }

            log.debug("Eliminando {} detalles del Carrito ID {}", detallesAEliminar.size(), carrito.getIdCarrito());
            detalleCarritoRepository.deleteAll(detallesAEliminar);


            carrito.getDetallesCarrito().clear();
            carritoRepository.save(carrito);
            log.info("Todos los detalles movidos y carrito vaciado.");

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

    private static class DetallePedidoInfo {
        Producto producto;
        Integer cantidad;
        BigDecimal subtotalConDescuento;
        BigDecimal montoDescuento;

        DetallePedidoInfo(Producto producto, Integer cantidad, BigDecimal subtotalConDescuento, BigDecimal montoDescuento) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.subtotalConDescuento = subtotalConDescuento;
            this.montoDescuento = montoDescuento;
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

        if (!pedido.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            log.warn("Acceso denegado: Usuario {} intentó acceder al pedido ID {} que pertenece a otro usuario.", usuarioEmail, pedidoId);
            throw new SecurityException("Acceso denegado: Este pedido no te pertenece.");
        }
        log.info("Pedido ID {} obtenido con éxito para usuario {}", pedidoId, usuarioEmail);
        return mapToPedidoResponse(pedido);
    }

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
            if (StringUtils.isBlank(request.getNuevoEstado())) {
                throw new IllegalArgumentException("El nuevo estado no puede estar vacío.");
            }
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
            pago.setMetodo(Pago.MetodoPago.TARJETA);
            crearPagoNuevo = true;
        }

        try {
            if (StringUtils.isBlank(request.getNuevoEstadoPago())) {
                throw new IllegalArgumentException("El nuevo estado de pago no puede estar vacío.");
            }
            Pago.EstadoPago nuevoEstadoPago = Pago.EstadoPago.valueOf(request.getNuevoEstadoPago().toUpperCase());
            pago.setEstado(nuevoEstadoPago);
            if (nuevoEstadoPago == Pago.EstadoPago.COMPLETADO && pago.getFechaPago() == null) {
                pago.setFechaPago(LocalDateTime.now());
                log.debug("Admin: Registrando fecha de pago para pedido ID {}", pedidoId);
            }
            Pago pagoGuardado = pagoRepository.save(pago);
            if(crearPagoNuevo) {
                pedido.setPago(pagoGuardado);
            }
            log.info("Admin: Estado de pago del pedido ID {} actualizado a {}", pedidoId, nuevoEstadoPago);

            boolean pedidoModificado = crearPagoNuevo;
            if (nuevoEstadoPago == Pago.EstadoPago.COMPLETADO && pedido.getEstado() == Pedido.EstadoPedido.PENDIENTE) {
                pedido.setEstado(Pedido.EstadoPedido.PAGADO);
                log.info("Admin: Estado del pedido ID {} actualizado a PAGADO.", pedidoId);
                pedidoModificado = true;
            }
            if (pedidoModificado) {
                pedidoRepository.save(pedido);
            }

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
            envio.setEstado(Envio.EstadoEnvio.EN_PREPARACION);
            crearEnvioNuevo = true;
        }

        boolean envioModificado = false;
        if (StringUtils.isNotBlank(request.getDireccionEnvio())) {
            envio.setDireccionEnvio(request.getDireccionEnvio());
            log.debug("Admin: Dirección de envío actualizada para pedido ID {}", pedidoId);
            envioModificado = true;
        }
        if (request.getFechaEnvio() != null) {
            envio.setFechaEnvio(request.getFechaEnvio());
            log.debug("Admin: Fecha de envío actualizada para pedido ID {}", pedidoId);
            envioModificado = true;
        }
        if (StringUtils.isNotBlank(request.getCodigoSeguimiento())) {
            envio.setCodigoSeguimiento(request.getCodigoSeguimiento());
            log.debug("Admin: Código de seguimiento actualizado para pedido ID {}", pedidoId);
            envioModificado = true;
        }

        boolean estadoPedidoModificado = false;
        if (StringUtils.isNotBlank(request.getNuevoEstadoEnvio())) {
            try {
                Envio.EstadoEnvio nuevoEstadoEnvio = Envio.EstadoEnvio.valueOf(request.getNuevoEstadoEnvio().toUpperCase());
                if (envio.getEstado() != nuevoEstadoEnvio) {
                    envio.setEstado(nuevoEstadoEnvio);
                    log.info("Admin: Estado de envío del pedido ID {} actualizado a {}", pedidoId, nuevoEstadoEnvio);
                    envioModificado = true;

                    if (nuevoEstadoEnvio == Envio.EstadoEnvio.ENTREGADO) {
                        pedido.setEstado(Pedido.EstadoPedido.ENTREGADO);
                        estadoPedidoModificado = true;
                        log.info("Admin: Estado del pedido ID {} actualizado a ENTREGADO.", pedidoId);
                    } else if (nuevoEstadoEnvio == Envio.EstadoEnvio.EN_CAMINO) {
                        if (pedido.getEstado() == Pedido.EstadoPedido.PAGADO || pedido.getEstado() == Pedido.EstadoPedido.ENVIADO) {
                            pedido.setEstado(Pedido.EstadoPedido.ENVIADO);
                            estadoPedidoModificado = true;
                            log.info("Admin: Estado del pedido ID {} actualizado a ENVIADO.", pedidoId);
                        }
                        if (envio.getFechaEnvio() == null) {
                            envio.setFechaEnvio(LocalDate.now());
                            log.debug("Admin: Registrando fecha de envío actual para pedido ID {}", pedidoId);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Admin: Estado de envío inválido recibido: '{}'", request.getNuevoEstadoEnvio(), e);
                throw new IllegalArgumentException("Estado de envío no válido: "+ request.getNuevoEstadoEnvio());
            }
        }

        if (envioModificado || crearEnvioNuevo) {
            Envio envioGuardado = envioRepository.save(envio);
            if(crearEnvioNuevo) {
                pedido.setEnvio(envioGuardado);
                estadoPedidoModificado = true;
            }
            log.debug("Admin: Entidad Envío guardada para pedido ID {}", pedidoId);
        }

        if (estadoPedidoModificado) {
            pedidoRepository.save(pedido);
            log.debug("Admin: Entidad Pedido guardada debido a cambios en envío para ID {}", pedidoId);
        }

        Pedido pedidoFinal = pedidoRepository.findById(pedidoId).get();
        return mapToPedidoResponse(pedidoFinal);
    }


    @Override
    @Transactional
    public void deletePedido(Integer pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));
        pedidoRepository.delete(pedido);
        log.info("Admin: Pedido ID {} eliminado exitosamente.", pedidoId);
    }

    //  Lógica de Mapeo (Helper)
    private PedidoResponse mapToPedidoResponse(Pedido pedido) {
        List<DetallePedido> detalles = pedido.getDetallesPedido();
        if (detalles == null) {
            detalles = new ArrayList<>();
            log.warn("La lista detallesPedido era null para Pedido ID {}, inicializando.", pedido.getIdPedido());
        } else {
            detalles.size();
        }

        List<DetallePedidoResponse> detallesResponse = detalles.stream()
                .map(detalle -> {
                    String nombreProducto = (detalle.getProducto() != null) ? detalle.getProducto().getNombre() : "Producto Desconocido";
                    Integer idProducto = (detalle.getProducto() != null) ? detalle.getProducto().getIdProducto() : -1;
                    BigDecimal precioOriginalUnitario = (detalle.getProducto() != null && detalle.getProducto().getPrecio() != null)
                            ? detalle.getProducto().getPrecio() : BigDecimal.ZERO;

                    return DetallePedidoResponse.builder()
                            .detallePedidoId(detalle.getIdDetallePedido())
                            .productoId(idProducto)
                            .productoNombre(nombreProducto)
                            .cantidad(detalle.getCantidad() != null ? detalle.getCantidad() : 0)
                            .precioUnitario(precioOriginalUnitario)
                            .subtotal(detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO)
                            .montoDescuento(detalle.getMontoDescuento() != null ? detalle.getMontoDescuento() : BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());

        Pago pago = pedido.getPago();
        Envio envio = pedido.getEnvio();
        Usuario usuario = pedido.getUsuario();

        // Crear el resumen del usuario
        PedidoResponse.UsuarioResumen usuarioResumen = null;
        if (usuario != null) {
            usuarioResumen = PedidoResponse.UsuarioResumen.builder()
                    .idUsuario(usuario.getIdUsuario())
                    .nombre(usuario.getNombre())
                    .email(usuario.getEmail())
                    .build();
        }

        return PedidoResponse.builder()
                .pedidoId(pedido.getIdPedido())
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : "DESCONOCIDO")
                .total(pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO)
                .detalles(detallesResponse)
                .direccionEnvio(envio != null ? envio.getDireccionEnvio() : "N/A")
                .estadoEnvio(envio != null && envio.getEstado() != null ? envio.getEstado().name() : "N/A")
                .estadoPago(pago != null && pago.getEstado() != null ? pago.getEstado().name() : "N/A")
                .metodoPago(pago != null && pago.getMetodo() != null ? pago.getMetodo().name() : "N/A")
                .usuario(usuarioResumen)
                .usuarioId(usuario != null ? usuario.getIdUsuario() : null)
                .build();
    }
}