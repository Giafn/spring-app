package com.tecnicaltest.spring_app.feature.auth.utils;

import com.tecnicaltest.spring_app.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "my-test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm",
                900000L,
                604800000L
        );
    }

    @Test
    void generateAccessTokenShouldReturnValidToken() {
        User user = User.builder().username("admin").name("Administrator").build();

        String token = jwtUtil.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void generateRefreshTokenShouldReturnValidToken() {
        User user = User.builder().username("admin").build();

        String token = jwtUtil.generateRefreshToken(user);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void extractUsernameShouldReturnCorrectUser() {
        User user = User.builder().username("testuser").name("Test User").build();
        String token = jwtUtil.generateAccessToken(user);

        String username = jwtUtil.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() {
        User user = User.builder().username("admin").build();
        String token = jwtUtil.generateAccessToken(user);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid-token"));
    }

    @Test
    void validateTokenShouldReturnFalseForTamperedToken() {
        User user = User.builder().username("admin").build();
        String token = jwtUtil.generateAccessToken(user);

        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void accessTokenAndRefreshTokenShouldBeDifferent() {
        User user = User.builder().username("admin").name("Administrator").build();

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        assertNotEquals(accessToken, refreshToken);
    }

    @Test
    void accessTokenExpirationShouldReturnConfiguredValue() {
        assertEquals(900000L, jwtUtil.getAccessTokenExpiration());
    }

    @Test
    void refreshTokenExpirationShouldReturnConfiguredValue() {
        assertEquals(604800000L, jwtUtil.getRefreshTokenExpiration());
    }
}
