package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombre(String name);
}
