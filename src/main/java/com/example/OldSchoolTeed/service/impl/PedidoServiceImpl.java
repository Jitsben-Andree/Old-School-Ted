package com.example.OldSchoolTeed.service.impl;

// IMPORTS DE DTOs (¡ESTOS SON LOS QUE PROBABLEMENTE TE FALTAN!)
import com.example.OldSchoolTeed.dto.AdminUpdateEnvioRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePagoRequest;
import com.example.OldSchoolTeed.dto.AdminUpdatePedidoStatusRequest;
import com.example.OldSchoolTeed.dto.DetallePedidoResponse; // <-- MUY IMPORTANTE
import com.example.OldSchoolTeed.dto.PedidoRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;      // <-- MUY IMPORTANTE

// IMPORTS DE ENTIDADES
import com.example.OldSchoolTeed.entities.Carrito;
import com.example.OldSchoolTeed.entities.DetalleCarrito; // <-- IMPORTANTE PARA EL REPO
import com.example.OldSchoolTeed.entities.DetallePedido;
import com.example.OldSchoolTeed.entities.Envio;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Pago;
import com.example.OldSchoolTeed.entities.Pedido;
import com.example.OldSchoolTeed.entities.Usuario;

// IMPORTS DE REPOSITORIOS (¡ESTOS TAMBIÉN TE PUEDEN FALTAR!)
import com.example.OldSchoolTeed.repository.CarritoRepository;
import com.example.OldSchoolTeed.repository.DetalleCarritoRepository; // <-- IMPORTANTE
import com.example.OldSchoolTeed.repository.DetallePedidoRepository;
import com.example.OldSchoolTeed.repository.EnvioRepository;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.PagoRepository;
import com.example.OldSchoolTeed.repository.PedidoRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;

// IMPORT DEL SERVICIO
import com.example.OldSchoolTeed.service.PedidoService;

// IMPORTS DE SPRING Y JAKARTA
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// IMPORTS DE JAVA UTIL
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements PedidoService {

    // --- Inyección de todos los Repositorios ---
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final InventarioRepository inventarioRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ProductoRepository productoRepository; // (Aunque no se use, es bueno tenerlo si se expande)

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
    @Transactional
    public PedidoResponse crearPedidoDesdeCarrito(String usuarioEmail, PedidoRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detallesCarrito = detalleCarritoRepository.findByCarrito(carrito);

        if (detallesCarrito.isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        // 1. Validar Stock
        for (DetalleCarrito detalle : detallesCarrito) {
            Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto())
                    .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para: " + detalle.getProducto().getNombre()));
            if (inventario.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + detalle.getProducto().getNombre());
            }
        }

        // 2. Crear Pedido
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(Pedido.EstadoPedido.Pendiente);

        // Calcular total
        BigDecimal total = detallesCarrito.stream()
                .map(detalle -> detalle.getProducto().getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotal(total);

        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 3. Crear Pago y Envío (iniciales)
        Pago pago = new Pago();
        pago.setPedido(pedidoGuardado);
        pago.setFechaPago(null); // Pendiente
        pago.setMetodo(Pago.MetodoPago.valueOf(request.getMetodoPagoInfo().toUpperCase()));
        pago.setMonto(total);
        pago.setEstado(Pago.EstadoPago.Pendiente);
        pagoRepository.save(pago);

        Envio envio = new Envio();
        envio.setPedido(pedidoGuardado);
        envio.setFechaEnvio(null); // Pendiente
        envio.setDireccionEnvio(request.getDireccionEnvio());
        envio.setEstado(Envio.EstadoEnvio.En_camino);
        envioRepository.save(envio);

        // 4. Mover detalles de Carrito a Pedido y Actualizar Stock
        for (DetalleCarrito detalle : detallesCarrito) {
            // Crear detalle de pedido
            DetallePedido detallePedido = new DetallePedido();
            detallePedido.setPedido(pedidoGuardado);
            detallePedido.setProducto(detalle.getProducto());
            detallePedido.setCantidad(detalle.getCantidad());
            detallePedido.setSubtotal(detalle.getProducto().getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            // detallePedido.setMontoDescuento(BigDecimal.ZERO); // (Lógica de descuento aquí)
            detallePedidoRepository.save(detallePedido);

            // Actualizar inventario
            Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto()).get();
            inventario.setStock(inventario.getStock() - detalle.getCantidad());
            inventarioRepository.save(inventario);

            // Eliminar de detalle_carrito
            detalleCarritoRepository.delete(detalle);
        }

        // (Llenar el Pago y Envio en el Pedido para el mapeo)
        pedidoGuardado.setPago(pago);
        pedidoGuardado.setEnvio(envio);

        return mapToPedidoResponse(pedidoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> getPedidosByUsuario(String usuarioEmail) {
        Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        return pedidoRepository.findByUsuario(usuario).stream()
                .map(this::mapToPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse getPedidoById(String usuarioEmail, Integer pedidoId) {
        Usuario usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));

        // Validar pertenencia
        if (pedido.getUsuario().getIdUsuario() != usuario.getIdUsuario()) {
            throw new SecurityException("Acceso denegado: Este pedido no te pertenece.");
        }

        return mapToPedidoResponse(pedido);
    }


    // --- Métodos de Administrador ---

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> getAllPedidosAdmin() {
        return pedidoRepository.findAll().stream()
                .map(this::mapToPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PedidoResponse updatePedidoStatusAdmin(Integer pedidoId, AdminUpdatePedidoStatusRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        // Convertir String a Enum (validando)
        try {
            Pedido.EstadoPedido nuevoEstado = Pedido.EstadoPedido.valueOf(request.getNuevoEstado().toUpperCase());
            pedido.setEstado(nuevoEstado);
            pedidoRepository.save(pedido);
            return mapToPedidoResponse(pedido);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de pedido no válido: " + request.getNuevoEstado());
        }
    }

    @Override
    @Transactional
    public PedidoResponse updatePagoStatusAdmin(Integer pedidoId, AdminUpdatePagoRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        Pago pago = pedido.getPago();
        if (pago == null) {
            throw new EntityNotFoundException("No se encontró información de pago para el pedido: " + pedidoId);
        }

        // Convertir String a Enum
        try {
            Pago.EstadoPago nuevoEstadoPago = Pago.EstadoPago.valueOf(request.getNuevoEstadoPago().toUpperCase());
            pago.setEstado(nuevoEstadoPago);
            pagoRepository.save(pago);

            // Lógica de negocio extra: Si el pago se COMPLETA, actualizamos el Pedido
            if (nuevoEstadoPago == Pago.EstadoPago.Completado) {
                pedido.setEstado(Pedido.EstadoPedido.Pagado);
                pedidoRepository.save(pedido);
            }

            return mapToPedidoResponse(pedido);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de pago no válido: " + request.getNuevoEstadoPago());
        }
    }

    @Override
    @Transactional
    public PedidoResponse updateEnvioDetailsAdmin(Integer pedidoId, AdminUpdateEnvioRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        Envio envio = pedido.getEnvio();
        if (envio == null) {
            throw new EntityNotFoundException("No se encontró información de envío para el pedido: " + pedidoId);
        }

        // Actualizar campos del Envío
        if (request.getDireccionEnvio() != null && !request.getDireccionEnvio().isEmpty()) {
            envio.setDireccionEnvio(request.getDireccionEnvio());
        }
        if (request.getFechaEnvio() != null) {
            envio.setFechaEnvio(request.getFechaEnvio());
        }

        // Actualizar estado del Envío (y potencialmente del Pedido)
        if (request.getNuevoEstadoEnvio() != null && !request.getNuevoEstadoEnvio().isEmpty()) {
            try {
                Envio.EstadoEnvio nuevoEstadoEnvio = Envio.EstadoEnvio.valueOf(request.getNuevoEstadoEnvio().toUpperCase());
                envio.setEstado(nuevoEstadoEnvio);

                // Lógica de negocio extra: Actualizar Pedido según el Envío
                if (nuevoEstadoEnvio == Envio.EstadoEnvio.Entregado) {
                    pedido.setEstado(Pedido.EstadoPedido.Enviado);
                } else if (nuevoEstadoEnvio == Envio.EstadoEnvio.Entregado) {
                    pedido.setEstado(Pedido.EstadoPedido.Entregado);
                }

                // Guardamos ambos
                envioRepository.save(envio);
                pedidoRepository.save(pedido);

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de envío no válido: "+ request.getNuevoEstadoEnvio());
            }
        } else {
            // Si no se cambió el estado, solo guardar el envío
            envioRepository.save(envio);
        }

        return mapToPedidoResponse(pedido);
    }


    // --- Lógica de Mapeo (Helper) ---
    private PedidoResponse mapToPedidoResponse(Pedido pedido) {
        // Mapear detalles
        List<DetallePedidoResponse> detallesResponse = pedido.getDetallesPedido().stream()
                .map(detalle -> DetallePedidoResponse.builder()
                        .detallePedidoId(detalle.getIdDetallePedido())
                        .productoId(detalle.getProducto().getIdProducto())
                        .productoNombre(detalle.getProducto().getNombre())
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getProducto().getPrecio())
                        .subtotal(detalle.getSubtotal())
                        .montoDescuento(detalle.getMontoDescuento())
                        .build())
                .collect(Collectors.toList());

        // Obtener info de pago y envío (si existen)
        Pago pago = pedido.getPago();
        Envio envio = pedido.getEnvio();

        return PedidoResponse.builder()
                .pedidoId(pedido.getIdPedido())
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado().name())
                .total(pedido.getTotal())
                .detalles(detallesResponse)
                .direccionEnvio(envio != null ? envio.getDireccionEnvio() : "N/A")
                .estadoEnvio(envio != null ? envio.getEstado().name() : "N/A")
                .estadoPago(pago != null ? pago.getEstado().name() : "N/A")
                .metodoPago(pago != null ? pago.getMetodo().name() : "N/A")
                .build();
    }
}