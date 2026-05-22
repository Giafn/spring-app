package com.tecnicaltest.spring_app.feature.auth.service;

import com.tecnicaltest.spring_app.feature.auth.dto.AuthResponse;
import com.tecnicaltest.spring_app.feature.auth.dto.LoginRequest;
import com.tecnicaltest.spring_app.feature.auth.utils.JwtUtil;
import com.tecnicaltest.spring_app.repository.RefreshTokenRepository;
import com.tecnicaltest.spring_app.repository.UserRepository;
import com.tecnicaltest.spring_app.feature.auth.service.impl.AuthServiceImpl;
import com.tecnicaltest.spring_app.entity.RefreshToken;
import com.tecnicaltest.spring_app.entity.User;
import com.tecnicaltest.spring_app.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtUtil = mock(JwtUtil.class);
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AuthServiceImpl(authenticationManager, jwtUtil, userRepository, refreshTokenRepository, passwordEncoder);
    }

    @Test
    void registerShouldSucceed() {
        var request = com.tecnicaltest.spring_app.feature.auth.dto.RegisterRequest.builder()
                .username("newuser")
                .password("secret123")
                .passwordConfirmation("secret123")
                .build();
        User saved = User.builder().id(1L).username("newuser").name("newuser").createdAt(java.time.Instant.now()).build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(saved);

        var resp = service.register(request);

        assertEquals(1L, resp.getId());
        assertEquals("newuser", resp.getUsername());
        assertEquals("newuser", resp.getName());
        assertNotNull(resp.getCreatedAt());
        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed")));
    }

    @Test
    void registerShouldThrowWhenPasswordMismatch() {
        var request = com.tecnicaltest.spring_app.feature.auth.dto.RegisterRequest.builder()
                .username("user")
                .password("secret")
                .passwordConfirmation("different")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldThrowWhenUsernameExists() {
        var request = com.tecnicaltest.spring_app.feature.auth.dto.RegisterRequest.builder()
                .username("existing")
                .password("secret123")
                .passwordConfirmation("secret123")
                .build();

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldReturnTokens() {
        LoginRequest request = LoginRequest.builder().username("admin").password("admin123").build();
        User user = User.builder().id(1L).username("admin").name("Administrator").build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = service.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(900000L, response.getExpiresIn());
        assertEquals("Bearer", response.getTokenType());
        verify(authenticationManager).authenticate(any());
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void loginWithBadCredentialsShouldThrow() {
        LoginRequest request = LoginRequest.builder().username("admin").password("wrong").build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> service.login(request));
        verify(userRepository, never()).findByUsername(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginWithUserNotFoundAfterAuthShouldThrow() {
        LoginRequest request = LoginRequest.builder().username("ghost").password("pass").build();

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.login(request));
        verify(authenticationManager).authenticate(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshShouldReturnNewTokens() {
        User user = User.builder().id(1L).username("admin").name("Administrator").build();
        RefreshToken stored = RefreshToken.builder()
                .token("old-refresh")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(stored));
        when(jwtUtil.validateToken("old-refresh")).thenReturn(true);
        when(jwtUtil.extractUsername("old-refresh")).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(user)).thenReturn("new-refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse response = service.refresh("old-refresh");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void refreshWithInvalidTokenShouldThrow() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.refresh("invalid"));
    }

    @Test
    void refreshWithExpiredTokenShouldThrowAndDelete() {
        User user = User.builder().id(1L).build();
        RefreshToken stored = RefreshToken.builder()
                .token("expired")
                .user(user)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> service.refresh("expired"));
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void refreshWithInvalidJwtShouldThrowAndDelete() {
        User user = User.builder().id(1L).build();
        RefreshToken stored = RefreshToken.builder()
                .token("bad-jwt")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("bad-jwt")).thenReturn(Optional.of(stored));
        when(jwtUtil.validateToken("bad-jwt")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.refresh("bad-jwt"));
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void refreshShouldThrowWhenUserNotFound() {
        User user = User.builder().id(1L).username("ghost").build();
        RefreshToken stored = RefreshToken.builder()
                .token("valid-token")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(stored));
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.refresh("valid-token"));
        verify(refreshTokenRepository, never()).delete(any());
    }
}
