package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.service.ScheduledTasksService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/admin/tasks")
@PreAuthorize("hasAuthority('Administrador')")
@Slf4j
public class AdminTaskController {

    private final ScheduledTasksService scheduledTasksService;

    public AdminTaskController(ScheduledTasksService scheduledTasksService) {
        this.scheduledTasksService = scheduledTasksService;
    }

    // Helper para convertir texto en archivo descargable
    private ResponseEntity<Resource> buildFileResponse(String reportContent, String baseFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = baseFileName + "_" + timestamp + ".txt";

        ByteArrayResource resource = new ByteArrayResource(reportContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    //  Limpieza de Tokens (Descarga Reporte)
    @PostMapping("/cleanup-tokens")
    public ResponseEntity<Resource> runCleanupTokens() {
        log.warn("ADMIN: Ejecución manual de limpieza.");
        String report = scheduledTasksService.ejecutarLimpiezaManual();
        return buildFileResponse(report, "reporte_limpieza");
    }

    // Cancelar Pedidos (Descarga Reporte)
    @PostMapping("/cancel-orders")
    public ResponseEntity<Resource> runCancelOrders() {
        log.warn("ADMIN: Ejecución manual de cancelación.");
        String report = scheduledTasksService.ejecutarCancelacionManual();
        return buildFileResponse(report, "reporte_stock_liberado");
    }

    // Reporte de Ventas (Descarga Reporte)
    @PostMapping("/sales-report")
    public ResponseEntity<Resource> runSalesReport() {
        log.warn("ADMIN: Generación manual de reporte ventas.");
        String report = scheduledTasksService.ejecutarReporteManual();
        return buildFileResponse(report, "reporte_ventas");
    }

    //  Backup de Base de Datos
    @PostMapping("/backup-db")
    public ResponseEntity<Map<String, String>> runBackupDb() {
        log.warn("ADMIN: Backup manual solicitado.");


        String logOutput = scheduledTasksService.ejecutarBackupDatabaseManual();

        log.info("Resultado del Backup:\n{}", logOutput);

        // Retornamos un JSON simple al frontend
        return ResponseEntity.ok(Map.of(
                "message", "Proceso de backup finalizado correctamente. Archivo guardado en el servidor."
        ));
    }
}