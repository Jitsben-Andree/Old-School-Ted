package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.auth.AuthResponse;
import com.example.OldSchoolTeed.dto.auth.LoginRequest;
import com.example.OldSchoolTeed.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    // Opcional: AuthResponse refreshToken(String refreshToken);
}