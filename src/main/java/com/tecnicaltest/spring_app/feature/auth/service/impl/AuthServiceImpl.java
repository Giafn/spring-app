package com.tecnicaltest.spring_app.feature.auth.service.impl;

import com.tecnicaltest.spring_app.feature.auth.dto.AuthResponse;
import com.tecnicaltest.spring_app.feature.auth.dto.LoginRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RegisterRequest;
import com.tecnicaltest.spring_app.feature.auth.dto.RegisterResponse;
import com.tecnicaltest.spring_app.feature.auth.utils.JwtUtil;
import com.tecnicaltest.spring_app.repository.RefreshTokenRepository;
import com.tecnicaltest.spring_app.repository.UserRepository;
import com.tecnicaltest.spring_app.feature.auth.service.AuthService;
import com.tecnicaltest.spring_app.entity.RefreshToken;
import com.tecnicaltest.spring_app.entity.User;
import com.tecnicaltest.spring_app.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        saveRefreshToken(user, refreshToken);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token expired");
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenRepository.delete(stored);
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        saveRefreshToken(user, newRefreshToken);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new IllegalArgumentException("Password and confirmation do not match");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getUsername())
                .build();
        User saved = userRepository.save(user);
        return RegisterResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .name(saved.getName())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private void saveRefreshToken(User user, String token) {
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTokenExpiration()))
                .build());
    }
}
