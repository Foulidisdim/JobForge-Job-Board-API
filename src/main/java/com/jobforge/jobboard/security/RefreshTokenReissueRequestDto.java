package com.jobforge.jobboard.security;

import lombok.Data;

// TRANSFERS THE REFRESH TOKEN to the AuthController's refresh endpoint to create a new ACCESS TOKEN
@Data
public class RefreshTokenReissueRequestDto {
    private String refreshToken;
}