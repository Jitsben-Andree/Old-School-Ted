package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.entities.Rol; // Importar Rol
import com.example.OldSchoolTeed.entities.Usuario; // Importar Usuario
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // Añadir Logger
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true) // Importante para cargar colecciones LAZY como roles
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Intentando cargar usuario por email: {}", email);

        // Spring Security llama "username" a nuestro "email"
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con email: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado con email: " + email);
                });

        // Convertimos nuestros Roles (entidad) a GrantedAuthority de Spring
        // Asegurarse de que la colección de roles se cargue (puede ser LAZY)
        Set<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> {
                    // Log para ver el nombre del rol antes de crear SimpleGrantedAuthority
                    log.trace("Mapeando rol: {} (ID: {})", rol.getNombre(), rol.getIdRol());
                    return new SimpleGrantedAuthority(rol.getNombre()); // Usar el nombre exacto del rol
                })
                .collect(Collectors.toSet());

        // *** LOG CLAVE: Imprimir las autoridades (roles) cargadas ***
        log.info("Usuario {} cargado con roles: {}", email, authorities);

        // Construir y devolver el UserDetails de Spring Security
        // Esta es la parte CRÍTICA que conecta tu lógica
        return new User(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getActivo(), // enabled (true = activo)
                true, // accountNonExpired
                true, // credentialsNonExpired
                usuario.isAccountNonLocked(), // accountNonLocked (true = no bloqueado)
                authorities
        );
    }
}