package com.example.OldSchoolTeed.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Guarda un archivo en el directorio de subidas.
     * @param file El archivo a guardar.
     * @return El nombre único del archivo guardado.
     * @throws IOException Si ocurre un error al guardar.
     */
    public String store(MultipartFile file) throws IOException {
        // 1. Crear el directorio de subidas si no existe
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Generar un nombre de archivo único para evitar colisiones
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // 3. Definir la ruta de destino y copiar el archivo
        Path destinationPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Devolver el nombre (o la ruta relativa)
        // Devolvemos solo el nombre, ya que serviremos los archivos estáticamente
        return uniqueFilename;
    }

    /**
     * Elimina un archivo si existe.
     * @param filename El nombre del archivo a eliminar.
     */
    public void delete(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            System.err.println("Error al eliminar el archivo: " + filename + " - " + e.getMessage());
        }
    }
}

