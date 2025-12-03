package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.entities.Producto; // Importar Producto
import com.example.OldSchoolTeed.repository.ProductoRepository; // Importar ProductoRepository

import jakarta.annotation.PostConstruct; // Para init()
import jakarta.persistence.EntityNotFoundException; // Para manejo de errores
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Para leer application.properties
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Para updateProductImageUrl
import org.springframework.util.StringUtils; // Para limpiar nombre de archivo
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID; // Para nombres únicos

@Service
public class StorageService  {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);


    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path fileStorageLocation;

    private final ProductoRepository productoRepository;

    public StorageService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }


    @PostConstruct
    public void init() throws IOException {
        try {
            // Construir la ruta absoluta
            this.fileStorageLocation = Paths.get(this.uploadDir).toAbsolutePath().normalize();
            log.info("Directorio de almacenamiento de archivos configurado en: {}", this.fileStorageLocation);
            // Crear el directorio si no existe
            Files.createDirectories(this.fileStorageLocation);
            log.info("Directorio de almacenamiento verificado/creado con éxito.");
        } catch (IOException ex) {
            log.error("Error al crear el directorio de almacenamiento de archivos en {}", this.uploadDir, ex);
            throw new IOException("No se pudo crear el directorio donde se guardarán los archivos subidos.", ex);
        }
    }


    public String storeFile(MultipartFile file) throws IOException {
        // Limpiar el nombre del archivo para evitar problemas de ruta (ej. ../../)
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        log.debug("Nombre original del archivo: {}", originalFilename);

        // Validar nombre de archivo (evitar nombres vacíos o inválidos)
        if (originalFilename.isEmpty()) {
            throw new IOException("Nombre de archivo inválido.");
        }
        if (originalFilename.contains("..")) {
            throw new IOException("Nombre de archivo contiene secuencia de ruta inválida: " + originalFilename);
        }

        // Crear un nombre único para evitar sobreescritura y colisiones
        String fileExtension = "";
        try {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } catch (Exception e) {
            log.warn("No se pudo determinar la extensión del archivo: {}", originalFilename);
            fileExtension = "";
        }
        // Usar UUID para asegurar unicidad
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        log.debug("Nombre de archivo único generado: {}", uniqueFilename);


        try {
            // Construir la ruta completa del archivo destino
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);
            log.debug("Ruta destino del archivo: {}", targetLocation);


            // Copiar el archivo al directorio destino
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                log.info("Archivo guardado exitosamente en: {}", targetLocation);
            }

            return uniqueFilename; // Devolver solo el nombre único generado

        } catch (IOException ex) {
            log.error("Error al guardar el archivo {}: {}", uniqueFilename, ex.getMessage(), ex);
            throw new IOException("No se pudo guardar el archivo " + uniqueFilename + ". Por favor intente de nuevo.", ex);
        }
    }


    public Resource loadFileAsResource(String filename) throws MalformedURLException, IOException {
        try {
            Path filePath = load(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("Archivo {} cargado como Resource desde {}", filename, filePath);
                return resource;
            } else {
                log.error("Archivo no encontrado o no legible en la ruta: {}", filePath);
                throw new IOException("Archivo no encontrado o no se puede leer: " + filename);
            }
        } catch (MalformedURLException ex) {
            log.error("URL mal formada al intentar cargar archivo {}: {}", filename, ex.getMessage(), ex);
            throw new MalformedURLException("No se pudo leer el archivo: " + filename);
        }
    }


    @Transactional // Necesario para modificar la entidad Producto
    public void updateProductImageUrl(Integer productId, String imageUrl) {
        log.info("Actualizando imageUrl para Producto ID {} a {}", productId, imageUrl);
        // Buscar el producto
        Producto producto = productoRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Producto no encontrado con ID {} al intentar actualizar imageUrl", productId);
                    return new EntityNotFoundException("Producto no encontrado con ID: " + productId);
                });

        // Actualizar la URL y guardar
        producto.setImageUrl(imageUrl);
        productoRepository.save(producto);
        log.info("ImageUrl actualizada con éxito para Producto ID {}", productId);
    }


    public Path load(String filename) {
        // Método helper para obtener la ruta completa
        return this.fileStorageLocation.resolve(filename).normalize();
    }
}
