package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.auth.AuthResponse;
import com.example.OldSchoolTeed.dto.auth.LoginRequest;
import com.example.OldSchoolTeed.dto.auth.RegisterRequest;
import com.example.OldSchoolTeed.dto.auth.UnlockRequest;


public interface AuthService {

    /**
     * Registra un nuevo usuario.
     * @param request Datos de registro
     * @return Respuesta de autenticación con tokens
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Autentica un usuario.
     * Implementa la lógica de bloqueo por intentos fallidos.
     * @param request Credenciales de login
     * @return Respuesta de autenticación con tokens
     */
    AuthResponse login(LoginRequest request);

    /**
     * Desbloquea una cuenta de usuario usando un código.
     * @param request Datos de desbloqueo (email, código, nueva contraseña)
     */
    void unlockAccount(UnlockRequest request);

    void sendRecoveryCode(String email);
}