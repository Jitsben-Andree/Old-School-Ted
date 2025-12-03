package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.auth.AuthResponse;
import com.example.OldSchoolTeed.dto.auth.LoginRequest;
import com.example.OldSchoolTeed.dto.auth.RegisterRequest;
import com.example.OldSchoolTeed.dto.auth.UnlockRequest;


public interface AuthService {

    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void unlockAccount(UnlockRequest request);

    void sendRecoveryCode(String email);
}