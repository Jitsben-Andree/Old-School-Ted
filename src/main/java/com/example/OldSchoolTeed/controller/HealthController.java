package com.example.OldSchoolTeed.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Slf4j
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getSystemStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("app", "UP");

        boolean dbUp = isDatabaseUp();
        status.put("database", dbUp ? "UP" : "DOWN");

        if (dbUp) {
            return ResponseEntity.ok(status);
        } else {
            log.error("🚨 ALERTA: La base de datos parece estar caída.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
    }


    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('Administrador')")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();

        // Conversión a MegaBytes
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        metrics.put("memory_total_mb", totalMemory);
        metrics.put("memory_used_mb", usedMemory);
        metrics.put("memory_free_mb", freeMemory);

        // Tiempo de actividad (Uptime)
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);
        String uptimeString = String.format("%d horas, %d minutos, %d segundos",
                uptime.toHours(), uptime.toMinutesPart(), uptime.toSecondsPart());

        metrics.put("uptime_human", uptimeString);
        metrics.put("uptime_millis", uptimeMillis);
        metrics.put("processors_available", runtime.availableProcessors());

        return ResponseEntity.ok(metrics);
    }

    // Método auxiliar
    private boolean isDatabaseUp() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}