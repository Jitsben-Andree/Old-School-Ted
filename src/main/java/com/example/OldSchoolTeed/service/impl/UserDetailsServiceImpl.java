package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.repository.UsuarioRepository;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class); // Añadir logger
    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Cargando usuario por email: {}", email);
        // Spring Security llama "username" a nuestro "email"
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado con email: {}", email);
                    return new UsernameNotFoundException("Usuario no encontrado con email: " + email);
                });

        // Convertimos nuestros Roles (entidad) a GrantedAuthority de Spring
        // ¡ASEGÚRATE DE QUE ESTO USA rol.getNombre() EXACTAMENTE!
        Set<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> {
                    log.trace("Mapeando rol: {} a GrantedAuthority", rol.getNombre());
                    // Spring Security a veces prefiere el prefijo "ROLE_"
                    // pero hasAnyAuthority no lo necesita. Dejémoslo sin prefijo por ahora.
                    return new SimpleGrantedAuthority(rol.getNombre());
                })
                .collect(Collectors.toSet());

        // Log para depurar qué roles se están cargando
        log.info("Usuario {} cargado con roles: {}", email, authorities);


        return new User(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getActivo(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
