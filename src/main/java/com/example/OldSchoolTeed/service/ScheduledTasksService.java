package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.repository.PedidoRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
public class ScheduledTasksService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final RestTemplate restTemplate;

    // UUIDs de Healthchecks.io
    private final String PING_URL_BASE = "https://hc-ping.com/";
    private final String UUID_LIMPIEZA = "e302d11c-2412-4e86-8bad-582adef7d8dc";
    private final String UUID_PEDIDOS = "a8b10650-4eda-4bee-a4d1-40b28eaa4fd3";
    private final String UUID_VENTAS = "2e8214d9-478e-4e90-a384-5763c290f5f8";
    private final String UUID_BACKUP = "3a789e80-25a6-4597-a4c5-1331d8dd0faa";

    private final String HEALTHCHECK_URL = "https://hc-ping.com/e302d11c-2412-4e86-8bad-582adef7d8dc";

    public ScheduledTasksService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.restTemplate = new RestTemplate();
    }

    // TAREAS AUTOMÁTICAS (@Scheduled)

    @Scheduled(cron = "0 0 4 * * *")
    public void limpiarCodigosDeDesbloqueo() {
        ejecutarLimpiezaManual();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cancelarPedidosPendientesAntiguos() {
        ejecutarCancelacionManual();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void reporteDiarioDeVentas() {
        ejecutarReporteManual();
    }

    // ========================================================================
    // TAREAS MANUALES (Retornan Reporte de Texto)
    // ========================================================================

    public String ejecutarLimpiezaManual() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE LIMPIEZA ===\n");
        monitorLogic("Limpieza Tokens", UUID_LIMPIEZA, sb, () -> {
            int afectados = usuarioRepository.limpiarCodigosVencidos(LocalDateTime.now());
            sb.append("Registros eliminados: ").append(afectados).append("\n");
        });
        return sb.toString();
    }

    public String ejecutarCancelacionManual() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE STOCK ===\n");
        monitorLogic("Cancelar Pedidos", UUID_PEDIDOS, sb, () -> {
            int cancelados = pedidoRepository.cancelarPedidosExpirados(LocalDateTime.now().minusHours(24));
            sb.append("Pedidos cancelados: ").append(cancelados).append("\n");
        });
        return sb.toString();
    }

    public String ejecutarReporteManual() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE VENTAS ===\n");
        monitorLogic("Reporte Ventas", UUID_VENTAS, sb, () -> {
            BigDecimal total = pedidoRepository.sumarVentasEnRango(
                    LocalDate.now().minusDays(1).atStartOfDay(),
                    LocalDate.now().minusDays(1).atTime(LocalTime.MAX)
            );
            sb.append("Total Vendido Ayer: $").append(total != null ? total : 0).append("\n");
        });
        return sb.toString();
    }

    /**
     * BACKUP ROBUSTO: Captura errores en el texto y NO lanza excepciones.
     */
    public String ejecutarBackupDatabaseManual() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LOG DE BACKUP DE BASE DE DATOS ===\n");
        sb.append("Fecha: ").append(LocalDateTime.now()).append("\n");
        sb.append("------------------------------------------\n");

        // Variable para controlar el éxito/fallo internamente
        boolean exito = false;

        log.info(" Iniciando tarea: Backup Database Manual");
        ping(UUID_BACKUP + "/start");

        try {
            String scriptPath = "scripts/backup-db.bat";
            File scriptFile = new File(scriptPath);

            sb.append("Directorio de ejecución Java: ").append(new File(".").getAbsolutePath()).append("\n");
            sb.append("Buscando script en: ").append(scriptFile.getAbsolutePath()).append("\n");

            if (!scriptFile.exists()) {
                sb.append(" ERROR CRÍTICO: No se encuentra el archivo backup-db.bat\n");
                sb.append("Asegúrate de crear la carpeta 'scripts' en la raíz del proyecto.\n");
                // No lanzamos throw, solo marcamos fallo
            } else {
                sb.append("Script encontrado. Ejecutando...\n");
                sb.append("---------------- CONSOLA ----------------\n");

                // Ejecutar con comillas por si hay espacios en la ruta
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "\"" + scriptFile.getAbsolutePath() + "\"");
                builder.directory(scriptFile.getParentFile());
                builder.redirectErrorStream(true);

                Process process = builder.start();

                // Capturar salida en tiempo real
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                        log.debug("BACKUP >> {}", line);
                    }
                }

                int exitCode = process.waitFor();
                sb.append("------------------------------------------\n");

                if (exitCode == 0) {
                    sb.append(" RESULTADO: ÉXITO. El código de salida fue 0.\n");
                    exito = true;
                } else {
                    sb.append(" RESULTADO: FALLÓ. El código de salida fue ").append(exitCode).append(".\n");
                    sb.append("TIP: Revisa si la ruta de PostgreSQL en el .bat es correcta.\n");
                }
            }

        } catch (Exception e) {
            sb.append("\n EXCEPCIÓN JAVA IMPREVISTA:\n");
            sb.append(e.getMessage()).append("\n");
            log.error("Error ejecutando backup", e);
        }

        // Reportar a Healthchecks.io
        if (exito) {
            ping(UUID_BACKUP);
            log.info(" Backup finalizado con éxito.");
        } else {
            ping(UUID_BACKUP + "/fail");
            log.error(" Backup finalizó con errores.");
        }

        return sb.toString();
    }

    // --- AUXILIARES ---

    @FunctionalInterface
    interface TaskLogic { void run() throws Exception; }

    private void monitorLogic(String nombre, String uuid, StringBuilder sb, TaskLogic tarea) {
        log.info("🚀 Tarea: {}", nombre);
        ping(uuid + "/start");
        try {
            tarea.run();
            ping(uuid);
            sb.append("Estado: ÉXITO \n");
        } catch (Exception e) {
            log.error("Fallo tarea {}", nombre, e);
            ping(uuid + "/fail");
            sb.append("Estado: FALLO  - ").append(e.getMessage()).append("\n");
        }
    }

    private void ping(String endpoint) {
        try {
            if (endpoint != null && !endpoint.contains("TU-UUID")) {
                restTemplate.getForObject(PING_URL_BASE + endpoint, String.class);
            }
        } catch (Exception e) { /* Silent fail */ }
    }
}