package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.service.ProductoService;
import com.example.OldSchoolTeed.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileController {

    private final StorageService storageService;
    private final ProductoService productoService;

    public FileController(StorageService storageService, ProductoService productoService) {
        this.storageService = storageService;
        this.productoService = productoService;
    }

    @PostMapping("/upload/producto/{productoId}")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @PathVariable Integer productoId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        log.info("POST /files/upload/producto/{} -> Archivo: {}", productoId, file.getOriginalFilename());

        // Validamos existencia (si falla lanza EntityNotFoundException, capturado globalmente)
        productoService.getProductoById(productoId);

        // Validación simple de tipo
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no soportado. Solo JPG, PNG, GIF.");
        }

        String filename = storageService.storeFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files/uploads/")
                .path(filename)
                .toUriString();

        storageService.updateProductImageUrl(productoId, fileDownloadUri);
        log.info("Imagen subida exitosamente para producto ID {}: {}", productoId, filename);

        return ResponseEntity.ok(Map.of(
                "message", "Archivo subido con éxito: " + filename,
                "imageUrl", fileDownloadUri
        ));
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws IOException {
        log.debug("GET /files/uploads/{}", filename);

        Resource file = storageService.loadFileAsResource(filename);
        String contentType = Files.probeContentType(file.getFile().toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(file);
    }
}