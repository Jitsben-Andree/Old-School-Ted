package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.auth.*;
import com.example.OldSchoolTeed.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Auth: Solicitud de registro para email: {}", request.getEmail());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Auth: Intento de login para email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody ResetRequest request) {
        log.info("Auth: Solicitud de reset password para email: {}", request.getEmail());
        authService.sendRecoveryCode(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Código de recuperación enviado."));
    }

    @PostMapping("/unlock")
    public ResponseEntity<?> unlockAccount(@RequestBody UnlockRequest request) {
        log.info("Auth: Intento de desbloqueo de cuenta para email: {}", request.getEmail());
        authService.unlockAccount(request);
        return ResponseEntity.ok(Map.of("message", "Cuenta desbloqueada exitosamente."));
    }
}