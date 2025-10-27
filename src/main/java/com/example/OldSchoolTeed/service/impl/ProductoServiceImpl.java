package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.ProductoRequest;
import com.example.OldSchoolTeed.dto.ProductoResponse;
import com.example.OldSchoolTeed.dto.PromocionSimpleDto;
import com.example.OldSchoolTeed.entities.Categoria;
import com.example.OldSchoolTeed.entities.Inventario;
import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.entities.Promocion;
import com.example.OldSchoolTeed.repository.CategoriaRepository;
import com.example.OldSchoolTeed.repository.InventarioRepository;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.repository.PromocionRepository;
import com.example.OldSchoolTeed.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// --- Imports para POI ---
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// --- Import Apache Commons ---
import org.apache.commons.lang3.StringUtils; // <<< IMPORT CORRECTO
// --- Fin Imports ---
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InventarioRepository inventarioRepository;
    private final PromocionRepository promocionRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               InventarioRepository inventarioRepository,
                               PromocionRepository promocionRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inventarioRepository = inventarioRepository;
        this.promocionRepository = promocionRepository;
    }

    @Transactional(readOnly = true)
    public ProductoResponse mapToProductoResponse(Producto producto) {
        log.trace("Mapeando Producto ID: {}", producto.getIdProducto());
        Inventario inventario = inventarioRepository.findByProducto(producto)
                .orElseGet(() -> {
                    log.warn("No se encontró inventario para Producto ID: {}, devolviendo stock 0.", producto.getIdProducto());
                    Inventario tempInv = new Inventario();
                    tempInv.setStock(0);
                    tempInv.setProducto(producto);
                    return tempInv;
                });

        BigDecimal precioOriginal = producto.getPrecio();
        BigDecimal precioConDescuento = precioOriginal;
        BigDecimal descuentoAplicado = BigDecimal.ZERO;
        String nombrePromocion = null;
        Promocion mejorPromocionAplicada = null;

        LocalDateTime now = LocalDateTime.now();
        log.debug("Buscando promociones activas para Producto ID: {} en fecha/hora: {}", producto.getIdProducto(), now);
        List<Promocion> promocionesActivas = promocionRepository.findActivePromocionesForProducto(producto.getIdProducto(), now);
        log.debug("Encontradas {} promociones activas para Producto ID: {}", promocionesActivas.size(), producto.getIdProducto());

        if (!promocionesActivas.isEmpty()) {
            Optional<Promocion> mejorPromocionOpt = promocionesActivas.stream()
                    .filter(p -> p.getDescuento() != null)
                    .max(Comparator.comparing(Promocion::getDescuento));

            if (mejorPromocionOpt.isPresent()) {
                mejorPromocionAplicada = mejorPromocionOpt.get();
                descuentoAplicado = mejorPromocionAplicada.getDescuento();
                nombrePromocion = mejorPromocionAplicada.getDescripcion();

                log.debug("Mejor promoción encontrada para Prod ID {}: Promo ID {}, Descuento: {}%",
                        producto.getIdProducto(), mejorPromocionAplicada.getIdPromocion(), descuentoAplicado);

                if (descuentoAplicado.compareTo(BigDecimal.ZERO) > 0 && descuentoAplicado.compareTo(new BigDecimal("100")) <= 0) {
                    BigDecimal factorDescuento = descuentoAplicado.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    BigDecimal montoDescuento = precioOriginal.multiply(factorDescuento);
                    precioConDescuento = precioOriginal.subtract(montoDescuento).setScale(2, RoundingMode.HALF_UP);
                    log.info(">>> Promoción aplicada a Producto ID {}: '{}' ({}%). Precio: {} -> {} <<<",
                            producto.getIdProducto(), nombrePromocion, descuentoAplicado, precioOriginal, precioConDescuento);
                } else {
                    log.warn("Descuento inválido ({}) encontrado para Promoción ID {} en Producto ID {}. No se aplicará descuento.",
                            descuentoAplicado, mejorPromocionAplicada.getIdPromocion(), producto.getIdProducto());
                    descuentoAplicado = BigDecimal.ZERO;
                    nombrePromocion = null;
                    precioConDescuento = precioOriginal;
                }
            } else {
                log.debug("No se encontró una promoción válida (con descuento > 0) para Producto ID {}", producto.getIdProducto());
            }
        } else {
            log.debug("No hay promociones activas en este momento para Producto ID {}", producto.getIdProducto());
        }

        List<PromocionSimpleDto> promocionesAsociadasDto = Collections.emptyList();
        try {
            Hibernate.initialize(producto.getPromociones());
            Set<Promocion> promocionesAsociadas = producto.getPromociones();
            if (promocionesAsociadas != null && !promocionesAsociadas.isEmpty()) {
                promocionesAsociadasDto = promocionesAsociadas.stream()
                        .map(promo -> PromocionSimpleDto.builder()
                                .idPromocion(promo.getIdPromocion())
                                .codigo(promo.getCodigo())
                                .descripcion(promo.getDescripcion())
                                .descuento(promo.getDescuento())
                                .activa(promo.isActiva())
                                .build())
                        .collect(Collectors.toList());
                log.trace("Mapeadas {} promociones asociadas para Producto ID {}", promocionesAsociadasDto.size(), producto.getIdProducto());
            } else {
                log.trace("Producto ID {} no tiene promociones asociadas.", producto.getIdProducto());
            }
        } catch (Exception e) {
            log.error("Error al inicializar o mapear promociones asociadas para Producto ID {}: {}", producto.getIdProducto(), e.getMessage(), e); // Loguear excepción completa
            promocionesAsociadasDto = Collections.emptyList();
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
                .precioOriginal(precioOriginal)
                .descuentoAplicado(descuentoAplicado)
                .nombrePromocion(nombrePromocion)
                .promocionesAsociadas(promocionesAsociadasDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosActivos() {
        log.info("Obteniendo todos los productos activos.");
        return productoRepository.findByActivoTrue().stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getAllProductosIncludingInactive() {
        log.info("Admin: Obteniendo todos los productos (activos e inactivos).");
        return productoRepository.findAll().stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Integer id) {
        log.info("Obteniendo producto por ID: {}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
        return mapToProductoResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> getProductosByCategoria(String nombreCategoria) {
        log.info("Obteniendo productos por categoría: {}", nombreCategoria);
        // Usar StringUtils.isBlank para validar entrada
        if (StringUtils.isBlank(nombreCategoria)){
            log.warn("Nombre de categoría vacío recibido.");
            return Collections.emptyList(); // Devolver lista vacía si el nombre es inválido
        }
        return productoRepository.findByCategoriaNombre(nombreCategoria).stream()
                .filter(p -> p.getActivo() != null && p.getActivo())
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        log.info("Admin: Creando nuevo producto con nombre: {}", request.getNombre());
        // Validar request null
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo.");
        }
        // Validar nombre usando StringUtils
        if (StringUtils.isBlank(request.getNombre())){
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
        }

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion()); // Descripcion puede ser null o vacía
        try {
            // Validar talla usando StringUtils
            if (StringUtils.isBlank(request.getTalla())) {
                throw new IllegalArgumentException("La talla no puede estar vacía.");
            }
            producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Talla inválida recibida: '{}'. Usando 'M' por defecto.", request.getTalla(), e);
            producto.setTalla(Producto.Talla.M);
        }
        if (request.getPrecio() == null || request.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Precio inválido recibido: {}", request.getPrecio());
            throw new IllegalArgumentException("El precio debe ser un valor positivo.");
        }
        producto.setPrecio(request.getPrecio());
        producto.setActivo(request.getActivo() != null ? request.getActivo() : true);
        producto.setCategoria(categoria);

        Producto productoGuardado = productoRepository.save(producto);
        log.info("Admin: Producto creado con ID: {}", productoGuardado.getIdProducto());

        inventarioRepository.findByProducto(productoGuardado).orElseGet(() -> {
            Inventario inventario = new Inventario();
            inventario.setProducto(productoGuardado);
            inventario.setStock(0);
            Inventario savedInventario = inventarioRepository.save(inventario);
            log.info("Admin: Inventario inicial creado para Producto ID: {}", productoGuardado.getIdProducto());
            return savedInventario;
        });

        return mapToProductoResponse(productoGuardado);
    }

    @Override
    @Transactional
    public ProductoResponse updateProducto(Integer id, ProductoRequest request) {
        log.info("Admin: Actualizando producto ID: {}", id);
        if (request == null) {
            throw new IllegalArgumentException("El request no puede ser nulo.");
        }
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        // Actualizar categoría si se proporciona un ID válido
        if (request.getCategoriaId() != null && (producto.getCategoria() == null || !request.getCategoriaId().equals(producto.getCategoria().getIdCategoria()))) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
            log.debug("Admin: Categoría actualizada para Producto ID: {}", id);
        }

        // Actualizar campos (con validaciones usando StringUtils)
        if(StringUtils.isNotBlank(request.getNombre())) {
            producto.setNombre(request.getNombre());
        }
        // Permitir descripción null o vacía
        producto.setDescripcion(request.getDescripcion());

        try {
            // Actualizar talla si se proporciona y es válida
            if (StringUtils.isNotBlank(request.getTalla())) {
                producto.setTalla(Producto.Talla.valueOf(request.getTalla().toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            log.error("Talla inválida recibida al actualizar Producto ID {}: '{}'. Manteniendo talla anterior.", id, request.getTalla(), e);
        }

        // Validar y actualizar precio si se proporciona
        if (request.getPrecio() != null) {
            if (request.getPrecio().compareTo(BigDecimal.ZERO) > 0) {
                producto.setPrecio(request.getPrecio());
            } else {
                log.warn("Intento de actualizar Producto ID {} con precio inválido: {}. Precio no actualizado.", id, request.getPrecio());
            }
        }
        // Actualizar activo si se proporciona en el request
        if (request.getActivo() != null) {
            producto.setActivo(request.getActivo());
        }

        Producto productoActualizado = productoRepository.save(producto);
        log.info("Admin: Producto ID {} actualizado.", id);
        return mapToProductoResponse(productoActualizado);
    }

    @Override
    @Transactional
    public void deleteProducto(Integer id) {
        log.info("Admin: Desactivando (soft delete) producto ID: {}", id);
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));

        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Admin: Producto ID {} desactivado.", id);
    }

    @Override
    @Transactional
    public void associatePromocionToProducto(Integer productoId, Integer promocionId) {
        log.info("Admin: Asociando Promoción ID {} a Producto ID {}", promocionId, productoId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + promocionId));

        producto.getPromociones().add(promocion);
        log.info("Admin: Asociación completada.");
    }

    @Override
    @Transactional
    public void disassociatePromocionFromProducto(Integer productoId, Integer promocionId) {
        log.info("Admin: Desasociando Promoción ID {} de Producto ID {}", promocionId, productoId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
        Promocion promocion = promocionRepository.findById(promocionId)
                .orElseThrow(() -> new EntityNotFoundException("Promoción no encontrada con ID: " + promocionId));

        producto.getPromociones().remove(promocion);
        log.info("Admin: Desasociación completada.");
    }

    @Override
    @Transactional(readOnly = true)
    public Resource exportProductosToExcel() throws IOException {
        log.info("Admin: Iniciando exportación de productos a Excel.");
        List<Producto> productos = productoRepository.findAll();
        log.debug("Se exportarán {} productos.", productos.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Productos");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            String[] headers = {"ID", "Nombre", "Descripción", "Talla", "Precio Base", "Categoría ID", "Categoría Nombre", "Stock", "Activo", "URL Imagen"};
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerCellStyle);
            }

            CellStyle currencyCellStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyCellStyle.setDataFormat(format.getFormat("S/ #,##0.00"));

            int rowIdx = 1;
            for (Producto prod : productos) {
                Row row = sheet.createRow(rowIdx++);
                int stock = inventarioRepository.findByProducto(prod).map(Inventario::getStock).orElse(0);
                Categoria cat = prod.getCategoria();
                Integer catId = (cat != null) ? cat.getIdCategoria() : null;
                String catNombre = (cat != null) ? cat.getNombre() : "N/A";

                row.createCell(0).setCellValue(prod.getIdProducto() != null ? prod.getIdProducto() : 0);
                row.createCell(1).setCellValue(prod.getNombre() != null ? prod.getNombre() : "");
                row.createCell(2).setCellValue(prod.getDescripcion() != null ? prod.getDescripcion() : "");
                row.createCell(3).setCellValue(prod.getTalla() != null ? prod.getTalla().name() : "");

                Cell cellPrecioBase = row.createCell(4);
                if (prod.getPrecio() != null) {
                    cellPrecioBase.setCellValue(prod.getPrecio().doubleValue());
                    cellPrecioBase.setCellStyle(currencyCellStyle);
                } else { cellPrecioBase.setCellValue("-"); }

                if(catId != null) row.createCell(5).setCellValue(catId); else row.createCell(5).setCellValue("N/A");
                row.createCell(6).setCellValue(catNombre);
                row.createCell(7).setCellValue(stock);
                row.createCell(8).setCellValue(prod.getActivo() != null && prod.getActivo() ? "Sí" : "No");
                row.createCell(9).setCellValue(prod.getImageUrl() != null ? prod.getImageUrl() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Archivo Excel generado en memoria con {} productos.", productos.size());
            return new ByteArrayResource(out.toByteArray());

        } catch (IOException e) {
            log.error("Error al generar el archivo Excel de productos", e);
            throw new IOException("Error al generar el archivo Excel", e);
        }
    }
}

