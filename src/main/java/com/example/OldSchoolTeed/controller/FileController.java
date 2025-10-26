package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.entities.Producto;
import com.example.OldSchoolTeed.repository.ProductoRepository;
import com.example.OldSchoolTeed.service.StorageService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@PreAuthorize("hasAuthority('Administrador')")
public class FileController {

    private final StorageService storageService;
    private final ProductoRepository productoRepository;

    public FileController(StorageService storageService, ProductoRepository productoRepository) {
        this.storageService = storageService;
        this.productoRepository = productoRepository;
    }

    @PostMapping("/upload/{productoId}")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer productoId
    ) {
        // 1. Buscar el producto
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

        // 2. Si el producto ya tiene una imagen, borrar la antigua
        if (producto.getImageUrl() != null) {
            String oldFilename = producto.getImageUrl().substring(producto.getImageUrl().lastIndexOf("/") + 1);
            storageService.delete(oldFilename);
        }

        // 3. Guardar el nuevo archivo
        String filename;
        try {
            filename = storageService.store(file);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al guardar el archivo: " + e.getMessage());
        }

        // 4. Crear la URL de acceso al archivo
        // Ej: http://localhost:8080/api/v1/files/uploads/nombre-archivo.jpg
        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/files/uploads/") // (Necesitaremos configurar esta ruta estática)
                .path(filename)
                .toUriString();

        // 5. Actualizar el producto con la nueva URL
        producto.setImageUrl(fileUrl);
        productoRepository.save(producto);

        return ResponseEntity.ok(fileUrl);
    }
}

