package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.repository.PedidoRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Slf4j
public class ScheduledTasksService {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    public ScheduledTasksService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    //Health Check ...
    @Scheduled(cron = "0 * * * * *")
    public void reportarEstadoDelSistema() {
        log.info("CRON HEALTH: Sistema activo.");
    }

    // Limpieza de Códigos ...
    @Scheduled(cron = "0 0 4 * * *")
    public void limpiarCodigosDeDesbloqueo() {
        log.info(" CRON JOB: Iniciando limpieza de códigos de desbloqueo vencidos...");
        try {
            int registrosAfectados = usuarioRepository.limpiarCodigosVencidos(LocalDateTime.now());
            if (registrosAfectados > 0) log.info("CRON JOB: Se limpiaron {} usuarios.", registrosAfectados);
        } catch (Exception e) {
            log.error(" CRON JOB ERROR: Falló la limpieza de códigos ooo", e);
        }
    }

    // Cancelar Pedidos ...
    @Scheduled(cron = "0 0 * * * *")
    public void cancelarPedidosPendientesAntiguos() {
        log.info(" CRON JOB: Buscando pedidos pendientes antiguos...");
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusHours(24);
            int pedidosCancelados = pedidoRepository.cancelarPedidosExpirados(fechaLimite);
            if (pedidosCancelados > 0) log.info(" CRON JOB: Se cancelaron {} pedidos expirados.", pedidosCancelados);
        } catch (Exception e) {
            log.error("CRON JOB ERROR: Falló la cancelación de pedidos.", e);
        }
    }


     // Reporte Diario de Ventas
    // Se ejecuta todos los días a las 8:00 AM.


    @Scheduled(cron = "0 0 8 * * *")
    public void reporteDiarioDeVentas() {
        log.info("💰 CRON JOB: Generando reporte de ventas de ayer...");

        try {
            // Definir rango: Ayer completo
            LocalDate ayer = LocalDate.now().minusDays(1);
            LocalDateTime inicio = ayer.atStartOfDay();
            LocalDateTime fin = ayer.atTime(LocalTime.MAX);

            BigDecimal totalVentas = pedidoRepository.sumarVentasEnRango(inicio, fin);

            // Manejo si no hubo ventas (null)
            if (totalVentas == null) {
                totalVentas = BigDecimal.ZERO;
            }

            log.info("============================================");
            log.info("REPORTE DE VENTAS DEL DIA: {}", ayer);
            log.info("Total Generado: ${}", totalVentas);
            log.info("============================================");

        } catch (Exception e) {
            log.error("CRON JOB ERROR: Falló el reporte de ventas.", e);
        }
    }
}