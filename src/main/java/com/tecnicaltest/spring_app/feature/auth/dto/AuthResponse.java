package com.tecnicaltest.spring_app.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;

    private String refreshToken;

    private long expiresIn;

    @Builder.Default
    private String tokenType = "Bearer";
}
