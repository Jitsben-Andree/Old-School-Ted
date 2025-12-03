package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.dto.PromocionSimpleDto;
import com.example.OldSchoolTeed.entities.*;
import com.example.OldSchoolTeed.repository.*;
import com.example.OldSchoolTeed.service.ProductoService;
import com.example.OldSchoolTeed.service.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioRepository inventarioRepository;
    private final PromocionRepository promocionRepository;
    private final StorageService storageService;

    // URL Base para imágenes (ajustada a tu configuración de API)
    private static final String BASE_URL = "http://localhost:8080/api/v1/uploads/";

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               InventarioRepository inventarioRepository,
                               PromocionRepository promocionRepository,
                               StorageService storageService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.promocionRepository = promocionRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public ProductoResponse mapToProductoResponse(Producto producto) {
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> {
                    Inventario tempInv = new Inventario();
                    tempInv.setStock(0);
                    tempInv.setProducto(producto);
                    return tempInv;
                });

        BigDecimal precioOriginal = producto.getPrecio();
        BigDecimal precioConDescuento = precioOriginal;
        BigDecimal descuentoAplicado = BigDecimal.ZERO;
        String nombrePromocion = null;

        LocalDateTime now = LocalDateTime.now();
        List<Promocion> promocionesActivas = promocionRepository.findActivePromocionesForProducto(producto.getIdProducto(), now);

        if (!promocionesActivas.isEmpty()) {
            Optional<Promocion> mejorPromocionOpt = promocionesActivas.stream()
                    .filter(p -> p.getDescuento() != null)
                    .max(Comparator.comparing(Promocion::getDescuento));

            if (mejorPromocionOpt.isPresent()) {
                Promocion mejor = mejorPromocionOpt.get();
                BigDecimal desc = mejor.getDescuento();
                if (desc.compareTo(BigDecimal.ZERO) > 0 && desc.compareTo(new BigDecimal("100")) <= 0) {
                    BigDecimal factor = desc.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal monto = precioOriginal.multiply(factor);
                    precioConDescuento = precioOriginal.subtract(monto).setScale(2, RoundingMode.HALF_UP);
                    descuentoAplicado = desc;
                    nombrePromocion = mejor.getDescripcion();
                }
            }
        }

        List<PromocionSimpleDto> promocionesAsociadasDto = Collections.emptyList();
        try {
            Hibernate.initialize(producto.getPromociones());
            if (producto.getPromociones() != null && !producto.getPromociones().isEmpty()) {
                promocionesAsociadasDto = producto.getPromociones().stream()
                        .map(promo -> PromocionSimpleDto.builder()
                                .idPromocion(promo.getIdPromocion())
                                .codigo(promo.getCodigo())
                                .descripcion(promo.getDescripcion())
                                .descuento(promo.getDescuento())
                                .activa(promo.isActiva())
                                .build())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error al mapear promociones", e);
        }

        // Mapeo Galería
        List<ProductoResponse.ImagenDto> galeria = new ArrayList<>();
        if (producto.getImagenes() != null) {
            galeria = producto.getImagenes().stream()
                    .map(img -> new ProductoResponse.ImagenDto(img.getId(), img.getUrl()))
                    .collect(Collectors.toList());
        }


        List<ProductoResponse.LeyendaDto> leyendasDto = new ArrayList<>();
        if (producto.getLeyendas() != null) {
            leyendasDto = producto.getLeyendas().stream()
                    .map(l -> new ProductoResponse.LeyendaDto(l.getId(), l.getNombre(), l.getNumero()))
                    .collect(Collectors.toList());
        }


        return ProductoResponse.builder()
                .id(producto.getIdProducto())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .talla(producto.getTalla() != null ? producto.getTalla().name() : "N/A")
                .precio(precioConDescuento)
                .activo(producto.getActivo())
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin Categoría")
                .stock(inventario.getStock())
                .imageUrl(producto.getImageUrl())
                .galeriaImagenes(galeria)
                .colorDorsal(producto.getColorDorsal())
                .leyendas(leyendasDto)
                // -----------------------------
                .precioOriginal(precioOriginal)
                .descuentoAplicado(descuentoAplicado)
                .nombrePromocion(nombrePromocion)
                .promocionesAsociadas(promocionesAsociadasDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosActivos() {
        return productoRepository.findByActivoTrue().stream().map(this::mapToProductoResponse).collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosIncludingInactive() {
        return productoRepository.findAll().stream().map(this::mapToProductoResponse).collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Integer id) {
        return mapToProductoResponse(productoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado")));
    }
    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(String cat) {
        if (StringUtils.isBlank(cat)) return Collections.emptyList();
        return productoRepository.findByCategoriaNombre(cat).stream().filter(p -> p.getActivo()).map(this::mapToProductoResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        if (request == null || StringUtils.isBlank(request.getNombre())) throw new IllegalArgumentException("Datos inválidos");

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada"));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        try {
            producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
        } catch (Exception e) {
            producto.setTalla(Producto.Talla.M);
        }
        producto.setActivo(request.getActivo() != null ? request.getActivo() : true);
        producto.setCategoria(categoria);


        producto.setColorDorsal(request.getColorDorsal() != null ? request.getColorDorsal() : "#000000");

        if (request.getLeyendas() != null && !request.getLeyendas().isEmpty()) {
            for (ProductoRequest.LeyendaDto dto : request.getLeyendas()) {
                if (StringUtils.isNotBlank(dto.getNombre()) && StringUtils.isNotBlank(dto.getNumero())) {
                    Leyenda leyenda = new Leyenda();
                    leyenda.setNombre(dto.getNombre().toUpperCase());
                    leyenda.setNumero(dto.getNumero());
                    leyenda.setProducto(producto); // Relación
                    producto.getLeyendas().add(leyenda);
                }
            }
        }


        Producto saved = productoRepository.save(producto);
        Inventario inv = new Inventario(); inv.setProducto(saved); inv.setStock(0); inventarioRepository.save(inv);
        return mapToProductoResponse(saved);
    }

    @Override
    @Transactional
    public ProductoResponse updateProducto(Integer id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        if (request.getNombre() != null) producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        if (request.getPrecio() != null) producto.setPrecio(request.getPrecio());
        if (request.getCategoriaId() != null) {
            Categoria c = categoriaRepository.findById(request.getCategoriaId()).orElseThrow();
            producto.setCategoria(c);
        }
        if (request.getActivo() != null) producto.setActivo(request.getActivo());
        try { if (request.getTalla() != null) producto.setTalla(Producto.Talla.valueOf(request.getTalla())); } catch (Exception e) {}


        if (request.getColorDorsal() != null) {
            producto.setColorDorsal(request.getColorDorsal());
        }


        if (request.getLeyendas() != null) {

            producto.getLeyendas().clear();


            for (ProductoRequest.LeyendaDto dto : request.getLeyendas()) {
                if (StringUtils.isNotBlank(dto.getNombre()) && StringUtils.isNotBlank(dto.getNumero())) {
                    Leyenda leyenda = new Leyenda();
                    leyenda.setNombre(dto.getNombre().toUpperCase());
                    leyenda.setNumero(dto.getNumero());
                    leyenda.setProducto(producto);
                    producto.getLeyendas().add(leyenda);
                }
            }
        }


        return mapToProductoResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void deleteProducto(Integer id) {
        Producto p = productoRepository.findById(id).orElseThrow();
        p.setActivo(false);
        productoRepository.save(p);
    }

    @Override
    @Transactional
    public void associatePromocionToProducto(Integer pid, Integer promid) {
        Producto p = productoRepository.findById(pid).orElseThrow();
        Promocion pr = promocionRepository.findById(promid).orElseThrow();
        p.getPromociones().add(pr);
        productoRepository.save(p);
    }
    @Override
    @Transactional
    public void disassociatePromocionFromProducto(Integer pid, Integer promid) {
        Producto p = productoRepository.findById(pid).orElseThrow();
        Promocion pr = promocionRepository.findById(promid).orElseThrow();
        p.getPromociones().remove(pr);
        productoRepository.save(p);
    }
    @Override
    @Transactional
    public ProductoResponse uploadProductImage(Integer id, MultipartFile file) {
        try {
            Producto p = productoRepository.findById(id).orElseThrow();
            p.setImageUrl(BASE_URL + storageService.storeFile(file));
            return mapToProductoResponse(productoRepository.save(p));
        } catch(Exception e) { throw new RuntimeException(e); }
    }
    @Override
    @Transactional
    public ProductoResponse uploadGalleryImage(Integer id, MultipartFile file) {
        try {
            Producto p = productoRepository.findById(id).orElseThrow();
            ImagenProducto img = new ImagenProducto();
            img.setUrl(BASE_URL + storageService.storeFile(file));
            img.setProducto(p);
            p.getImagenes().add(img);
            return mapToProductoResponse(productoRepository.save(p));
        } catch(Exception e) { throw new RuntimeException(e); }
    }
    @Override
    @Transactional
    public void deleteGalleryImage(Integer pid, Integer imgId) {
        Producto p = productoRepository.findById(pid).orElseThrow();
        p.getImagenes().removeIf(i -> i.getId().equals(imgId));
        productoRepository.save(p);
    }
    @Override
    public Resource exportProductosToExcel() throws IOException { return new ByteArrayResource(new byte[0]); }
}