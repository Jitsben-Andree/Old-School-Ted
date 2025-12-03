package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.unlockCode = null, u.unlockCodeExpiration = null WHERE u.unlockCodeExpiration < :now")
    int limpiarCodigosVencidos(LocalDateTime now);

}
