package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.service.ProductoService; // Asegúrate que esté importado
import com.example.OldSchoolTeed.service.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path; // Asegúrate que Path esté importado
import java.util.Map;

@RestController
@RequestMapping("/files") // Ruta base para archivos
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final StorageService storageService;
    private final ProductoService productoService; // Necesario para buscar producto

    public FileController(StorageService storageService, ProductoService productoService) {
        this.storageService = storageService;
        this.productoService = productoService; // Asignar
    }


    // Endpoint para subir la imagen de un producto.
     //Solo accesible por Administradores (verificado en SecurityConfig).

    @PostMapping("/upload/producto/{productoId}")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable Integer productoId,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Recibida solicitud POST /files/upload/producto/{} para archivo: {}", productoId, file.getOriginalFilename());

        // Validar si el producto existe (importante)
        try {
            // Usar el servicio para validar existencia
            productoService.getProductoById(productoId);
            log.debug("Producto ID {} encontrado.", productoId);
        } catch (EntityNotFoundException e) {
            log.warn("Intento de subir imagen para producto no existente ID: {}", productoId);
            // Devolver 404 si el producto no existe
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado con ID: " + productoId);
        } catch (Exception e) {
            // Capturar otros errores al buscar producto
            log.error("Error al buscar Producto ID {} antes de subir imagen: {}", productoId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al verificar producto", e);
        }


        // Validar tipo de archivo (opcional pero recomendado)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
            log.warn("Intento de subir archivo no soportado: {}", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no soportado. Solo se permiten JPG, PNG, GIF.");
        }

        try {
            //  Guardar el archivo usando StorageService
            String filename = storageService.storeFile(file);
            log.info("Archivo guardado como: {}", filename);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/files/uploads/") // Añadir el path del endpoint de descarga
                    .path(filename)          // Añadir el nombre del archivo
                    .toUriString();           // Construir la URL completa

            log.debug("URL generada para el archivo: {}", fileDownloadUri);
            // *** FIN CORRECCIÓN ***


            // 3. Actualizar la entidad Producto con la nueva imageUrl
            storageService.updateProductImageUrl(productoId, fileDownloadUri); // Usar el método del servicio
            log.info("Actualizada imageUrl para Producto ID {} a: {}", productoId, fileDownloadUri);


            // 4. Devolver respuesta exitosa con la URL
            return ResponseEntity.ok(Map.of(
                    "message", "Archivo subido con éxito: " + filename,
                    "imageUrl", fileDownloadUri // Devolver la URL corregida
            ));

        } catch (IOException e) {
            log.error("Error al guardar el archivo para Producto ID {}: {}", productoId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo.", e);
        } catch (Exception e) { // Capturar otras posibles excepciones (ej. al actualizar producto)
            log.error("Error inesperado al procesar la subida para Producto ID {}: {}", productoId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error inesperado al procesar la subida.", e);
        }
    }


     //Endpoint público para servir/descargar archivos subidos.

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        log.debug("Solicitud GET /files/uploads/{} recibida", filename);
        try {
            Resource file = storageService.loadFileAsResource(filename);
            String contentType = Files.probeContentType(file.getFile().toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            log.debug("Sirviendo archivo {} con Content-Type: {}", filename, contentType);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(file);
        } catch (MalformedURLException e) {
            log.error("Error de URL mal formada para archivo {}", filename, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL de archivo inválida", e);
        } catch (IOException e) {
            log.error("Error al leer archivo {} o determinar Content-Type: {}", filename, e.getMessage()); // Loguear mensaje de IOEx
            // Si el StorageService lanza IOException por no encontrar, devolver 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado o no se pudo leer: " + filename, e);
        } catch (Exception e) {
            log.error("Error inesperado al servir archivo {}", filename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al servir archivo", e);
        }
    }
}

