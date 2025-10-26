package com.example.OldSchoolTeed.service.impl;


import com.example.OldSchoolTeed.dto.auth.AuthResponse;
import com.example.OldSchoolTeed.dto.auth.LoginRequest;
import com.example.OldSchoolTeed.dto.auth.RegisterRequest;
import com.example.OldSchoolTeed.entities.Rol;
import com.example.OldSchoolTeed.entities.Usuario;
import com.example.OldSchoolTeed.repository.RolRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import com.example.OldSchoolTeed.service.AuthService;
import com.example.OldSchoolTeed.service.JwtService;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class); // Añadir logger
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    // Cambiado para usar la interfaz UserDetailsService directamente
    private final UserDetailsService userDetailsService;


    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           // Inyectar la interfaz UserDetailsService
                           UserDetailsService userDetailsService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService; // Guardar la interfaz
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intentando registrar usuario con email: {}", request.getEmail());
        // 1. Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro fallido: El email {} ya está en uso", request.getEmail());
            throw new IllegalArgumentException("El email ya está en uso");
        }

        // 2. Buscar el rol "Cliente" o crearlo si no existe (con el nombre exacto)
        Rol rolCliente = rolRepository.findByNombre("Cliente")
                .orElseGet(() -> {
                    log.info("Rol 'Cliente' no encontrado, creándolo...");
                    return rolRepository.save(new Rol("Cliente"));
                });
        log.debug("Rol 'Cliente' obtenido/creado: ID {}", rolCliente.getIdRol());


        Set<Rol> roles = new HashSet<>();
        roles.add(rolCliente);

        // 3. Crear el nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(roles); // ¡Asegúrate de asignar los roles aquí!
        usuario.setActivo(true); // Asegúrate de que el usuario esté activo

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario {} registrado con éxito con ID {}", usuarioGuardado.getEmail(), usuarioGuardado.getIdUsuario());


        // 4. Generar tokens (usando UserDetails)
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuarioGuardado.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        log.debug("Tokens generados para el nuevo usuario {}", usuarioGuardado.getEmail());


        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .email(usuarioGuardado.getEmail())
                .nombre(usuarioGuardado.getNombre())
                .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Intentando login para usuario: {}", request.getEmail());
        // 1. Autenticar al usuario
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        log.info("Autenticación exitosa para {}", request.getEmail());

        // 2. Cargar UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        // 3. Generar tokens
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        log.debug("Tokens generados para {}", request.getEmail());


        // 4. Obtener el nombre (buscando en BD para asegurar)
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Usuario autenticado {} no encontrado en la BD después del login!", request.getEmail());
                    return new RuntimeException("Error inesperado después del login");
                });


        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .build();
    }
}
