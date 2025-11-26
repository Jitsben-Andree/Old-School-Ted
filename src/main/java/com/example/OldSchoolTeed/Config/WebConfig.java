package com.example.OldSchoolTeed.Config; // Asegúrate que el package coincida con tu estructura

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry; // Importante para Angular
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Obtenemos la ruta absoluta para evitar problemas con rutas relativas
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // CORRECCIÓN AQUÍ:
        // Cambiamos "/files/uploads/**" por "/uploads/**" para coincidir con ProductoServiceImpl
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath.toString() + "/");
    }

    // Agregamos CORS globalmente para evitar problemas de "Bloqueado por CORS" en las imágenes o API
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}