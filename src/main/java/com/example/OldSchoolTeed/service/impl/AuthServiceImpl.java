package com.example.OldSchoolTeed.service.impl;

import com.example.OldSchoolTeed.dto.auth.AuthResponse;
import com.example.OldSchoolTeed.dto.auth.LoginRequest;
import com.example.OldSchoolTeed.dto.auth.RegisterRequest;
import com.example.OldSchoolTeed.dto.auth.UnlockRequest;
import com.example.OldSchoolTeed.entities.Rol;
import com.example.OldSchoolTeed.entities.Usuario;
import com.example.OldSchoolTeed.repository.RolRepository;
import com.example.OldSchoolTeed.repository.UsuarioRepository;
import com.example.OldSchoolTeed.service.AuthService;
import com.example.OldSchoolTeed.service.EmailService;
import com.example.OldSchoolTeed.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           UserDetailsService userDetailsService,
                           EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Intentando registrar usuario con email: {}", request.getEmail());
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro fallido: El email {} ya está en uso", request.getEmail());
            throw new IllegalArgumentException("El email ya está en uso");
        }

        Rol rolCliente = rolRepository.findByNombre("Cliente")
                .orElseGet(() -> {
                    log.info("Rol 'Cliente' no encontrado, creándolo...");
                    return rolRepository.save(new Rol("Cliente"));
                });
        log.debug("Rol 'Cliente' obtenido/creado: ID {}", rolCliente.getIdRol());

        Set<Rol> roles = new HashSet<>();
        roles.add(rolCliente);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setRoles(roles);
        usuario.setActivo(true);
        usuario.setAccountNonLocked(true);
        usuario.setFailedLoginAttempts(0);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        log.info("Usuario {} registrado con éxito con ID {}", usuarioGuardado.getEmail(), usuarioGuardado.getIdUsuario());

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
    @Transactional(noRollbackFor = { BadCredentialsException.class, LockedException.class })
    public AuthResponse login(LoginRequest request) {
        log.info("Intentando login para usuario: {}", request.getEmail());

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!usuario.isAccountNonLocked()) {
            log.warn("Usuario {} con cuenta bloqueada intentó iniciar sesión.", request.getEmail());
            throw new LockedException("Tu cuenta está bloqueada. Por favor, restablece tu contraseña.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            if (usuario.getFailedLoginAttempts() > 0) {
                usuario.setFailedLoginAttempts(0);
                usuario.setUnlockCode(null);
                usuario.setUnlockCodeExpiration(null);
                usuarioRepository.save(usuario);
            }

            log.info("Autenticación exitosa para {}", request.getEmail());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String jwtToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            return AuthResponse.builder()
                    .token(jwtToken)
                    .refreshToken(refreshToken)
                    .email(usuario.getEmail())
                    .nombre(usuario.getNombre())
                    .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Credenciales incorrectas para: {}", request.getEmail());
            increaseFailedAttempts(usuario);
            throw new BadCredentialsException("Credenciales incorrectas");
        }
    }

    private void increaseFailedAttempts(Usuario user) {
        int newFailAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newFailAttempts);
        log.warn("Intento fallido #{} para {}", newFailAttempts, user.getEmail());

        if (newFailAttempts >= MAX_FAILED_ATTEMPTS) {
            lockUserAccount(user);
        } else {
            usuarioRepository.save(user);
        }
    }


    private void lockUserAccount(Usuario user) {
        user.setAccountNonLocked(false);
        user.setFailedLoginAttempts(0);

        //  LÓGICA DE CÓDIGO Y EMAIL ELIMINADA

        usuarioRepository.save(user);
        log.warn("CUENTA BLOQUEADA para: {}. No se envía código automático.", user.getEmail());
    }


    @Override
    @Transactional
    public void sendRecoveryCode(String email) {
        log.info("Solicitud de recuperación de cuenta para: {}", email);
        Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("El correo no está registrado."));

        // Generar código y guardarlo
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setUnlockCode(code);
        user.setUnlockCodeExpiration(LocalDateTime.now().plusMinutes(15)); // Válido por 15 minutos

        // Bloqueamos la cuenta para forzar el uso del código
        user.setAccountNonLocked(false);

        usuarioRepository.save(user);
        log.warn("Código de recuperación generado para: {}.", user.getEmail());

        // Enviar el correo de RECUPERACIÓN
        try {
            // Llama al método renombrado en EmailService
            emailService.sendRecoveryCodeEmail(user.getEmail(), code);
        } catch (Exception e) {
            log.error("Error al enviar email de recuperación: {}", e.getMessage());
            // Lanzamos la excepción para que el frontend sepa que falló
            throw new RuntimeException("Error al enviar el email: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void unlockAccount(UnlockRequest request) {
        log.info("Intento de desbloqueo/reseteo para: {}", request.getEmail());
        Usuario user = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getUnlockCode() == null || !user.getUnlockCode().equals(request.getCode())) {
            log.warn("Intento fallido de desbloqueo: Código incorrecto para {}", request.getEmail());
            throw new RuntimeException("Código incorrecto");
        }

        if (user.getUnlockCodeExpiration().isBefore(LocalDateTime.now())) {
            log.warn("Intento fallido de desbloqueo: Código expirado para {}", request.getEmail());
            throw new RuntimeException("El código ha expirado");
        }

        // Éxito: Limpiar todo, desbloquear y setear nueva contraseña
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setUnlockCode(null);
        user.setUnlockCodeExpiration(null);

        if (request.getNewPassword() == null || request.getNewPassword().isEmpty() || request.getNewPassword().length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        usuarioRepository.save(user);
        log.info("Cuenta desbloqueada y contraseña reseteada exitosamente para: {}", request.getEmail());
    }
}