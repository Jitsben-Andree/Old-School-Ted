package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Carrito;
import com.example.OldSchoolTeed.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Importar Query
import org.springframework.data.repository.query.Param; // Importar Param
import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Integer> {
    // Buscar el carrito de un usuario, trayendo los detalles en la misma consulta
    @Query("SELECT c FROM Carrito c LEFT JOIN FETCH c.detallesCarrito dc LEFT JOIN FETCH dc.producto WHERE c.usuario = :usuario")
    Optional<Carrito> findByUsuarioWithDetails(@Param("usuario") Usuario usuario);

    // Método original (puede causar N+1 queries si detallesCarrito es LAZY)
    Optional<Carrito> findByUsuario(Usuario usuario);
}
