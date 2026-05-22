package com.tecnicaltest.spring_app.feature.auth.service;

import com.tecnicaltest.spring_app.feature.auth.dto.AuthResponse;
import com.tecnicaltest.spring_app.feature.auth.dto.LoginRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RegisterRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RegisterResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    RegisterResponse register(RegisterRequest request);
}
