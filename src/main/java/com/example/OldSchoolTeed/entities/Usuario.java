package com.example.OldSchoolTeed.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "fecha_registro", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime fechaRegistro;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Indica si la cuenta está bloqueada (false) o no (true).
     * Spring Security usará esto (a través de UserDetails) para denegar el acceso.
     */
    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    /**
     * Contador de intentos fallidos de login consecutivos.
     */
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    /**
     * Código de un solo uso para desbloquear la cuenta.
     */
    @Column(name = "unlock_code")
    private String unlockCode;

    /**
     * Fecha y hora en que expira el código de desbloqueo.
     */
    @Column(name = "unlock_code_expiration")
    private LocalDateTime unlockCodeExpiration;



    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<Rol> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }

    /**
     * IMPORTANTE:
     * Asegúrate de que tu clase UserDetailsServiceImpl (o donde implementes
     * UserDetailsService) esté mapeando correctamente estos campos.
     * Específicamente, el método UserDetails.isAccountNonLocked()
     * debe devolver el valor de este campo 'accountNonLocked'.
     * Y UserDetails.isEnabled() debe devolver 'activo'.
     */
}