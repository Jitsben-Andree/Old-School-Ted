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
    private final InventarioRepository inventarioRepository;
    private final ProductoService productoService;

    public CarritoServiceImpl(CarritoRepository carritoRepository,
                              DetalleCarritoRepository detalleCarritoRepository,
                              UsuarioRepository usuarioRepository,
                              ProductoRepository productoRepository,
                              InventarioRepository inventarioRepository,
                              ProductoService productoService) {
        this.carritoRepository = carritoRepository;
        this.detalleCarritoRepository = detalleCarritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
        this.productoService = productoService;
    }

    //  LÓGICA DE MAPEO (Con Datos de Personalización)
    private CarritoResponse mapToCarritoResponse(Carrito carrito) {
        log.trace("Mapeando Carrito ID: {}", carrito.getIdCarrito());
        List<DetalleCarrito> detalles = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : Collections.emptyList();

        List<DetalleCarritoResponse> itemResponses = detalles.stream()
                .map(detalle -> {
                    ProductoResponse productoConDescuento;
                    String imageUrl = null;
                    int stockActual = 0;

                    try {
                        productoConDescuento = productoService.getProductoById(detalle.getProducto().getIdProducto());
                        imageUrl = productoConDescuento.getImageUrl();
                        Inventario inventario = inventarioRepository.findByProducto(detalle.getProducto()).orElse(null);
                        if (inventario != null) stockActual = inventario.getStock();
                    } catch (EntityNotFoundException e) {
                        productoConDescuento = ProductoResponse.builder()
                                .id(detalle.getProducto().getIdProducto())
                                .nombre(detalle.getProducto().getNombre() + " (No disponible)")
                                .precio(BigDecimal.ZERO).precioOriginal(BigDecimal.ZERO).build();
                    }

                    // Usar el subtotal inteligente de la entidad
                    BigDecimal subtotalReal = detalle.getSubtotal();

                    // Fallback para items antiguos sin precio base
                    if (detalle.getPrecioBase() == null) {
                        subtotalReal = productoConDescuento.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad()));
                    }

                    return DetalleCarritoResponse.builder()
                            .detalleCarritoId(detalle.getIdDetalleCarrito())
                            .productoId(detalle.getProducto().getIdProducto())
                            .productoNombre(detalle.getProducto().getNombre())
                            .cantidad(detalle.getCantidad())
                            .precioUnitario(detalle.getPrecioBase() != null ? detalle.getPrecioBase() : productoConDescuento.getPrecio())
                            .subtotal(subtotalReal)
                            .imageUrl(imageUrl)
                            .stockActual(stockActual)

                            // --- DATOS DE PERSONALIZACIÓN ---
                            .personalizacionTipo(detalle.getPersonalizacionTipo())
                            .personalizacionNombre(detalle.getPersonalizacionNombre())
                            .personalizacionNumero(detalle.getPersonalizacionNumero())
                            .personalizacionPrecio(detalle.getPersonalizacionPrecio())
                            .parcheTipo(detalle.getParcheTipo())
                            .parchePrecio(detalle.getParchePrecio())
                            // -------------------------------
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalConDescuento = itemResponses.stream().map(DetalleCarritoResponse::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponse.builder()
                .carritoId(carrito.getIdCarrito())
                .usuarioId(carrito.getUsuario().getIdUsuario())
                .items(itemResponses)
                .total(totalConDescuento)
                .build();
    }

    private Carrito getOrCreateCarrito(Usuario usuario) {
        Optional<Carrito> carritoOpt = carritoRepository.findByUsuario(usuario);
        return carritoOpt.orElseGet(() -> {
            Carrito nuevoCarrito = new Carrito();
            nuevoCarrito.setUsuario(usuario);
            nuevoCarrito.setDetallesCarrito(new ArrayList<>());
            return carritoRepository.save(nuevoCarrito);
        });
    }

    @Override
    @Transactional
    public CarritoResponse getCarritoByUsuario(String userEmail) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = getOrCreateCarrito(usuario);
        return mapToCarritoResponse(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse addItemToCarrito(String userEmail, @Valid AddItemRequest request) {
        log.info("Añadiendo item: User {}, Prod {}, Cant {}", userEmail, request.getProductoId(), request.getCantidad());

        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Producto producto = productoRepository.findById(request.getProductoId()).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        if (request.getCantidad() == null || request.getCantidad() <= 0) throw new RuntimeException("Cantidad debe ser mayor a 0");

        Inventario inventario = inventarioRepository.findByProducto(producto).orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado"));
        Carrito carrito = getOrCreateCarrito(usuario);
        List<DetalleCarrito> detallesActuales = carrito.getDetallesCarrito() != null ? carrito.getDetallesCarrito() : new ArrayList<>();

        //  LÓGICA DE AGRUPACIÓN INTELIGENTE
        boolean esPersonalizado = request.getPersonalizacion() != null || request.getParche() != null;
        DetalleCarrito itemExistente = null;

        // Solo buscamos agrupar si NO es personalizado
        if (!esPersonalizado) {
            itemExistente = detallesActuales.stream()
                    .filter(d -> d.getProducto().getIdProducto().equals(request.getProductoId())
                            && d.getPersonalizacionTipo() == null
                            && d.getParcheTipo() == null)
                    .findFirst().orElse(null);
        }

        // Validación de Stock Global
        int cantidadEnCarrito = detallesActuales.stream()
                .filter(d -> d.getProducto().getIdProducto().equals(request.getProductoId()))
                .mapToInt(DetalleCarrito::getCantidad)
                .sum();

        if (inventario.getStock() < (cantidadEnCarrito + request.getCantidad())) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + inventario.getStock());
        }

        if (itemExistente != null) {
            itemExistente.setCantidad(itemExistente.getCantidad() + request.getCantidad());
            detalleCarritoRepository.save(itemExistente);
        } else {
            DetalleCarrito nuevo = new DetalleCarrito();
            nuevo.setCarrito(carrito);
            nuevo.setProducto(producto);
            nuevo.setCantidad(request.getCantidad());
            nuevo.setPrecioBase(producto.getPrecio());

            // Guardar Personalización
            if (request.getPersonalizacion() != null) {
                nuevo.setPersonalizacionTipo(request.getPersonalizacion().getTipo());
                nuevo.setPersonalizacionNombre(request.getPersonalizacion().getNombre());
                nuevo.setPersonalizacionNumero(request.getPersonalizacion().getNumero());
                nuevo.setPersonalizacionPrecio(request.getPersonalizacion().getPrecio());
            }
            // Guardar Parche
            if (request.getParche() != null) {
                nuevo.setParcheTipo(request.getParche().getTipo());
                nuevo.setParchePrecio(request.getParche().getPrecio());
            }

            DetalleCarrito guardado = detalleCarritoRepository.save(nuevo);
            if (carrito.getDetallesCarrito() == null) carrito.setDetallesCarrito(new ArrayList<>());
            carrito.getDetallesCarrito().add(guardado);
        }

        carritoRepository.save(carrito);
        return mapToCarritoResponse(carritoRepository.findById(carrito.getIdCarrito()).get());
    }

    @Override
    @Transactional
    public CarritoResponse removeItemFromCarrito(String userEmail, Integer detalleCarritoId) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuario(usuario).orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado"));

        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId).orElseThrow(() -> new EntityNotFoundException("Item no encontrado"));

        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            throw new SecurityException("Acceso denegado");
        }

        if(carrito.getDetallesCarrito() != null) carrito.getDetallesCarrito().removeIf(d -> d.getIdDetalleCarrito().equals(detalleCarritoId));
        detalleCarritoRepository.delete(detalle);

        return mapToCarritoResponse(carritoRepository.findById(carrito.getIdCarrito()).get());
    }

    @Override
    @Transactional
    public CarritoResponse updateItemQuantity(String userEmail, Integer detalleCarritoId, @Valid UpdateCantidadRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        Carrito carrito = carritoRepository.findByUsuario(usuario).orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado"));
        DetalleCarrito detalle = detalleCarritoRepository.findById(detalleCarritoId).orElseThrow(() -> new EntityNotFoundException("Item no encontrado"));

        if (!detalle.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            throw new SecurityException("Acceso denegado");
        }

        Inventario inv = inventarioRepository.findByProducto(detalle.getProducto()).orElseThrow(() -> new EntityNotFoundException("Inventario no encontrado"));

        // Validar stock global
        int otrosItems = carrito.getDetallesCarrito().stream()
                .filter(d -> d.getProducto().getIdProducto().equals(detalle.getProducto().getIdProducto()) && !d.getIdDetalleCarrito().equals(detalleCarritoId))
                .mapToInt(DetalleCarrito::getCantidad)
                .sum();

        if (inv.getStock() < (otrosItems + request.getNuevaCantidad())) {
            throw new RuntimeException("Stock insuficiente");
        }

        detalle.setCantidad(request.getNuevaCantidad());
        detalleCarritoRepository.save(detalle);

        return mapToCarritoResponse(carritoRepository.findById(carrito.getIdCarrito()).get());
    }
}