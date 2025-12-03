package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Pedido;
import com.example.OldSchoolTeed.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    // Método para buscar todos los pedidos de un usuario específico
    List<Pedido> findByUsuario(Usuario usuario);

    @Modifying
    @Transactional
    @Query("UPDATE Pedido p SET p.estado = 'CANCELADO' WHERE p.estado = 'PENDIENTE' AND p.fecha < :fechaLimite")
    int cancelarPedidosExpirados(LocalDateTime fechaLimite);

    @Query("SELECT SUM(p.total) FROM Pedido p WHERE p.fecha BETWEEN :inicio AND :fin AND p.estado IN ('PAGADO', 'ENVIADO', 'ENTREGADO')")
    BigDecimal sumarVentasEnRango(LocalDateTime inicio, LocalDateTime fin);

}