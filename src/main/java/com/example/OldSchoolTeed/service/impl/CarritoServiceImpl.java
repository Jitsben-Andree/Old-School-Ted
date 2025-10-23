package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.AddItemRequest;
import com.example.OldSchoolTeed.dto.CarritoResponse;
import com.example.OldSchoolTeed.dto.DetalleCarritoResponse;
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.CarritoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CarritoServiceImpl implements CarritoService {

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
        List<DetalleCarritoResponse> itemResponses = carrito.getDetallesCarrito().stream()
                .map(detalle -> DetalleCarritoResponse.builder()
                        .detalleCarritoId(detalle.getIdDetalleCarrito())
                        .productoId(detalle.getProducto().getIdProducto())
                        .productoNombre(detalle.getProducto().getNombre())
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getProducto().getPrecio())
                        .subtotal(detalle.getProducto().getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(DetalleCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponse.builder()
                .carritoId(carrito.getIdCarrito())
                .usuarioId(carrito.getUsuario().getIdUsuario())
                .items(itemResponses)
                .total(total)
                .build();
    }

    // --- Lógica de Negocio (Helper) ---
    private Carrito getOrCreateCarrito(Usuario usuario) {
        return carritoRepository.findByUsuario(usuario)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito();
                    nuevoCarrito.setUsuario(usuario);
                    nuevoCarrito.setFechaCreacion(LocalDateTime.now());
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CarritoResponse getCarritoByUsuario(String userEmail) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = getOrCreateCarrito(usuario);
        return mapToCarritoResponse(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse addItemToCarrito(String userEmail, AddItemRequest request) {
        // 1. Obtener usuario y producto
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        // 2. Validar Stock
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado para el producto"));
        if (inventario.getStock() < request.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Stock disponible: " + inventario.getStock());
        }

        // 3. Obtener o crear carrito
        Carrito carrito = getOrCreateCarrito(usuario);

        // 4. Verificar si el producto ya está en el carrito
        Optional<DetalleCarrito> itemExistente = carrito.getDetallesCarrito().stream()
                .filter(detalle -> detalle.getProducto().getIdProducto().equals(request.getProductoId()))
                .findFirst();

        if (itemExistente.isPresent()) {
            // 5.a. Actualizar cantidad si ya existe
            DetalleCarrito detalle = itemExistente.get();
            int nuevaCantidad = detalle.getCantidad() + request.getCantidad();
            // Re-validar stock total
            if (inventario.getStock() < nuevaCantidad) {
                throw new RuntimeException("Stock insuficiente para la cantidad total. Stock disponible: " + inventario.getStock());
            }
            detalle.setCantidad(nuevaCantidad);
            detalleCarritoRepository.save(detalle);
        } else {
            // 5.b. Añadir nuevo item si no existe
            DetalleCarrito nuevoDetalle = new DetalleCarrito();
            nuevoDetalle.setCarrito(carrito);
            nuevoDetalle.setProducto(producto);
            nuevoDetalle.setCantidad(request.getCantidad());
            detalleCarritoRepository.save(nuevoDetalle);
            // Añadir a la colección en memoria (opcional pero bueno para el response)
            carrito.getDetallesCarrito().add(nuevoDetalle);
        }

        return mapToCarritoResponse(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId) {
        // 1. Obtener usuario y carrito
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = getOrCreateCarrito(usuario);

        // 2. Encontrar el detalle del carrito
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId)
                .orElseThrow(() -> new EntityNotFoundException("Item del carrito no encontrado"));

        // 3. Validar que el item pertenezca al carrito del usuario
        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            throw new SecurityException("Acceso denegado: Este item no pertenece a tu carrito.");
        }

        // 4. Eliminar el item
        detalleCarritoRepository.delete(detalle);

        // 5. Recargar el carrito para devolverlo actualizado
        Carrito carritoActualizado = carritoRepository.findById(carrito.getIdCarrito()).get();
        return mapToCarritoResponse(carritoActualizado);
    }
}