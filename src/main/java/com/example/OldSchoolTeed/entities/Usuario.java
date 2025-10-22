package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private int idUsuario;

    @Column(name = "nombre",length = 100, nullable = false)
    private String nombre;

    @Column(name = "password_hash", length = 255 , nullable = false)
    private String passwordHash;

    @Column(name = "email",length = 100,nullable = false)
    private String email;

}
