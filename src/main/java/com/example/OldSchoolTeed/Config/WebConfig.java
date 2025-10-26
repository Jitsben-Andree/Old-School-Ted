package com.example.OldSchoolTeed.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Inyectamos la misma propiedad que usamos en StorageService
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Mapea las solicitudes de recursos estáticos (imágenes) a la
     * ubicación física en el disco.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Esto mapea la URL /files/uploads/**
        registry.addResourceHandler("/files/uploads/**")
                // A la ubicación física en el disco (ej: file:./uploads/)
                // ¡Asegúrate de que 'uploadDir' termine con '/' si es necesario!
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
