package com.tecnicaltest.spring_app.feature.auth.controller;

import com.tecnicaltest.spring_app.config.RateLimit;
import com.tecnicaltest.spring_app.feature.auth.dto.AuthResponse;
import com.tecnicaltest.spring_app.feature.auth.dto.LoginRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RefreshTokenRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RegisterRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RegisterResponse;
import com.tecnicaltest.spring_app.feature.auth.service.AuthService;
import com.tecnicaltest.spring_app.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication APIs")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @RateLimit(maxRequests = 3, windowSeconds = 60)
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", response));
    }

    @PostMapping("/login")
    @RateLimit(maxRequests = 3, windowSeconds = 60)
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }
}
