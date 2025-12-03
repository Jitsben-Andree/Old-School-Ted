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

    private final String LOG_FILE_PATH = "./logs/app.log";

    @GetMapping("/recent")
    public ResponseEntity<List<String>> getRecentLogs() throws IOException {
        Path path = Paths.get(LOG_FILE_PATH);
        File file = path.toFile();

        if (!file.exists()) {
            return ResponseEntity.ok(Collections.singletonList("⚠️ Archivo de log no encontrado aún."));
        }

        // Usamos try-with-resources solo para cerrar el Stream
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            List<String> recentLogs = lines.collect(Collectors.toList());

            if (recentLogs.size() > 100) {
                recentLogs = recentLogs.subList(recentLogs.size() - 100, recentLogs.size());
            }
            Collections.reverse(recentLogs);
            return ResponseEntity.ok(recentLogs);
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