package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Carrito;
import com.example.OldSchoolTeed.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Integer> {
    //buscar el carrito de un usuario
    Optional<Carrito> findByUsuario(Usuario usuario);
}
