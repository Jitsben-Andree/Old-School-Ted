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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;


    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           UserDetailsServiceImpl userDetailsService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        // 2. Buscar el rol "Cliente" o crearlo si no existe
        Rol rolCliente = rolRepository.findByNombre("Cliente")
                .orElseGet(() -> rolRepository.save(new Rol("Cliente")));

        Set<Rol> roles = new HashSet<>();
        roles.add(rolCliente);

        // 3. Crear el nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(roles);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        // 4. Generar tokens (usando UserDetails)
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Autenticar al usuario
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Si la autenticación fue exitosa, buscar al usuario
        // (Usamos UserDetailsService para obtener UserDetails)
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        // 3. Generar tokens
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // 4. Obtener el nombre del usuario (opcional, para la respuesta)
        // Podríamos castear UserDetails a User de Spring o buscar en la BD
        // Pero para evitar otra consulta, lo sacamos del UserDetails si es posible
        // O lo buscamos (más seguro para obtener el nombre)
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElseThrow();


        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .build();
    }
}