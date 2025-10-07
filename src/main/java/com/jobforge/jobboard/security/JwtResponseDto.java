package com.jobforge.jobboard.security;

import lombok.Builder;
import lombok.Data;

/// Holds the two tokens to return to the client upon successful login/.
@Data
@Builder
public class JwtResponseDto {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String userEmail;
}