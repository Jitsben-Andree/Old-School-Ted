package com.example.OldSchoolTeed.controller;

import com.example.OldSchoolTeed.dto.auth.AuthResponse;
import com.example.OldSchoolTeed.dto.auth.LoginRequest;
import com.example.OldSchoolTeed.dto.auth.RegisterRequest;
import com.example.OldSchoolTeed.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request
    ){
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (IllegalArgumentException e) {
            // x si el Email duplicado
            return ResponseEntity.badRequest().body(AuthResponse.builder().email(e.getMessage()).build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request
    ) {
        // El AuthenticationManager (dentro de authService.login)
        // arrojará BadCredentialsException si falla,
        // lo cual será manejado por un ExceptionHandler global (que podemos crear después)
        // Por ahora, un try-catch simple.
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(AuthResponse.builder().email("Credenciales incorrectas").build());
        }
    }

    // en  opcional: Endpoint para Refresh Token, extensionxd
    /*
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody RefreshTokenRequest request
    ) {
        // ... lógica para validar refresh token y emitir uno nuevo ...
    }
    */
}
