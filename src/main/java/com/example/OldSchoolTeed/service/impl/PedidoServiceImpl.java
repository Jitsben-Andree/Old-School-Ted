package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.DetallePedidoResponse;
import com.example.OldSchoolTeed.dto.PedidoRequest;
import com.example.OldSchoolTeed.dto.PedidoResponse;
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.PedidoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final DetalleCarritoRepository detalleCarritoRepository;
    private final InventarioRepository inventarioRepository;
    private final PagoRepository pagoRepository;
    private final EnvioRepository envioRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             DetallePedidoRepository detallePedidoRepository,
                             UsuarioRepository usuarioRepository,
                             CarritoRepository carritoRepository,
                             DetalleCarritoRepository detalleCarritoRepository,
                             InventarioRepository inventarioRepository,
                             PagoRepository pagoRepository,
                             EnvioRepository envioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.inventarioRepository = inventarioRepository;
        this.pagoRepository = pagoRepository;
        this.envioRepository = envioRepository;
    }

    // --- Lógica de Mapeo (Helper) ---
    private PedidoResponse mapToPedidoResponse(Pedido pedido) {
        // Mapear detalles
        List<DetallePedidoResponse> detallesResponse = pedido.getDetallesPedido().stream()
                .map(detalle -> DetallePedidoResponse.builder()
                        // --- INICIO DE CAMBIOS ---
                        .detallePedidoId(detalle.getIdDetallePedido()) // Nombre corregido
                        .productoId(detalle.getProducto().getIdProducto())
                        .productoNombre(detalle.getProducto().getNombre())
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getProducto().getPrecio()) // Obtenemos el precio del producto
                        .subtotal(detalle.getSubtotal())
                        .montoDescuento(detalle.getMontoDescuento()) // Campo añadido
                        // --- FIN DE CAMBIOS ---
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
                .detalles(detallesResponse) // Ahora usa el DTO corregido
                .direccionEnvio(envio != null ? envio.getDireccionEnvio() : "N/A")
                .estadoEnvio(envio != null ? envio.getEstado().name() : "N/A")
                .estadoPago(pago != null ? pago.getEstado().name() : "N/A")
                .metodoPago(pago != null ? pago.getMetodo().name() : "N/A")
                .build();
    }


    @Override
    @Transactional
    public PedidoResponse crearPedidoDesdeCarrito(String userEmail, PedidoRequest request) {
        // 1. Obtener usuario y su carrito
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado"));

        List<DetalleCarrito> detallesCarrito = carrito.getDetallesCarrito();
        if (detallesCarrito.isEmpty()) {
            throw new RuntimeException("No se puede crear un pedido de un carrito vacío");
        }

        // 2. Validar Stock y Calcular Total (¡Operación Crítica!)
        // Usamos una lista para guardar los detalles validados
        List<DetallePedido> detallesPedido = new ArrayList<>();
        BigDecimal totalPedido = BigDecimal.ZERO;

        for (DetalleCarrito detalleCarrito : detallesCarrito) {
            Producto producto = detalleCarrito.getProducto();
            int cantidadPedida = detalleCarrito.getCantidad();

            Inventario inventario = inventarioRepository.findByProducto(producto)
                    .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para: " + producto.getNombre()));

            if (inventario.getStock() < cantidadPedida) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre() + ". Stock: " + inventario.getStock());
            }

            // 3. Reducir el stock (¡Importante!)
            inventario.setStock(inventario.getStock() - cantidadPedida);
            inventarioRepository.save(inventario);

            // 4. Crear el DetallePedido
            DetallePedido detallePedido = new DetallePedido();
            detallePedido.setProducto(producto);
            detallePedido.setCantidad(cantidadPedida);
            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidadPedida));
            detallePedido.setSubtotal(subtotal);

            detallesPedido.add(detallePedido);
            totalPedido = totalPedido.add(subtotal);
        }

        // 5. Crear el Pedido principal
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(Pedido.EstadoPedido.Pendiente); // El pedido inicia como Pendiente
        pedido.setTotal(totalPedido);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // 6. Asociar los detalles al pedido
        for (DetallePedido detalle : detallesPedido) {
            detalle.setPedido(pedidoGuardado);
            detallePedidoRepository.save(detalle);
        }
        pedidoGuardado.setDetallesPedido(detallesPedido);

        // 7. Crear el Pago (simulado)
        Pago pago = new Pago();
        pago.setPedido(pedidoGuardado);
        pago.setFechaPago(LocalDateTime.now());
        // Simulamos un pago con Tarjeta que está Pendiente de confirmación
        pago.setMetodo(Pago.MetodoPago.Tarjeta);
        pago.setMonto(totalPedido);
        pago.setEstado(Pago.EstadoPago.Pendiente);
        pagoRepository.save(pago);
        pedidoGuardado.setPago(pago);

        // 8. Crear el Envío
        Envio envio = new Envio();
        envio.setPedido(pedidoGuardado);
        envio.setFechaEnvio(null); // Aún no se envía
        envio.setDireccionEnvio(request.getDireccionEnvio());
        envio.setEstado(Envio.EstadoEnvio.En_preparacion);
        envioRepository.save(envio);
        pedidoGuardado.setEnvio(envio);

        // 9. Vaciar el Carrito (¡Importante!)
        detalleCarritoRepository.deleteAll(detallesCarrito);

        // 10. Devolver la respuesta
        return mapToPedidoResponse(pedidoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> getPedidosByUsuario(String userEmail) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        List<Pedido> pedidos = pedidoRepository.findByUsuario(usuario);

        return pedidos.stream()
                .map(this::mapToPedidoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse getPedidoById(String userEmail, Integer pedidoId) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));

        // Validar que el pedido pertenece al usuario
        if (!pedido.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new SecurityException("Acceso denegado: Este pedido no te pertenece.");
        }

        return mapToPedidoResponse(pedido);
    }
}