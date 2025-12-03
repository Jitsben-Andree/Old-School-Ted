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
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Intentando cargar usuario por email: {}", email);


        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con email: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado con email: " + email);
                });



        Set<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> {

                    log.trace("Mapeando rol: {} (ID: {})", rol.getNombre(), rol.getIdRol());
                    return new SimpleGrantedAuthority(rol.getNombre());
                })
                .collect(Collectors.toSet());


        log.info("Usuario {} cargado con roles: {}", email, authorities);

        // Construir y devolver el UserDetails de Spring Security
        return new User(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getActivo(),
                true,
                true,
                usuario.isAccountNonLocked(),
                authorities
        );
    }
}