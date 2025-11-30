package com.example.OldSchoolTeed.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/logs")
@PreAuthorize("hasAuthority('Administrador')")
@Slf4j
public class LogController {

    // Ruta al archivo que configuramos en logback-spring.xml
    private final String LOG_FILE_PATH = "./logs/app.log";

    @GetMapping("/recent")
    public ResponseEntity<List<String>> getRecentLogs() {
        Path path = Paths.get(LOG_FILE_PATH);
        File file = path.toFile();

        if (!file.exists()) {
            return ResponseEntity.ok(Collections.singletonList(" Archivo de log no encontrado. ¿Ya se generaron eventos?"));
        }

        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            // Leer el archivo y quedarse con las últimas 50 líneas
            // Nota: Para archivos GIGANTES (GBs) esto no es eficiente, pero para 10MB está perfecto.
            List<String> recentLogs = lines
                    .collect(Collectors.toList());

            // Si hay muchas líneas, tomamos las últimas 100
            if (recentLogs.size() > 100) {
                recentLogs = recentLogs.subList(recentLogs.size() - 100, recentLogs.size());
            }

            // Invertimos para ver lo más nuevo arriba (opcional)
            Collections.reverse(recentLogs);

            return ResponseEntity.ok(recentLogs);

        } catch (IOException e) {
            log.error("Error leyendo logs", e);
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error leyendo el archivo de logs."));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadLogFile() {
        File file = new File(LOG_FILE_PATH);
        if (!file.exists()) return ResponseEntity.notFound().build();

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + file.getName())
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }
}