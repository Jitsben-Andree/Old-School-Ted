package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.auth.*;
import com.example.OldSchoolTeed.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map; // Para respuestas de error simples

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (IllegalArgumentException e) {
            // Email duplicado
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno al registrar."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (LockedException e) {
            // 403 Forbidden - Específico para cuenta bloqueada
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "CUENTA_BLOQUEADA"));
        } catch (BadCredentialsException e) {
            // 401 Unauthorized - Credenciales incorrectas
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en el servidor: " + e.getMessage()));
        }
    }


     //Endpoint para PASO 1 (Olvidé Contraseña) o para REENVIAR código.
     //Recibe un email y envía un código.

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody ResetRequest request) {
        try {
            authService.sendRecoveryCode(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Código de recuperación enviado."));
        } catch (Exception e) {
            // Ej: "El correo no está registrado."
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


     // Endpoint para PASO 2 (Desbloqueo / Seteo de nueva contraseña).
     //Recibe email, código y nueva contraseña.

    @PostMapping("/unlock")
    public ResponseEntity<?> unlockAccount(@RequestBody UnlockRequest request) {
        try {
            authService.unlockAccount(request);
            return ResponseEntity.ok(Map.of("message", "Cuenta desbloqueada exitosamente."));
        } catch (Exception e) {
            // Ej: "Código incorrecto", "Código ha expirado"
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}