package com.jobforge.jobboard.security;

import com.jobforge.jobboard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth") //authentication-specific functionality
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;


    //Endpoint to renew an expired Access Token.
    // The client sends the long-lived Refresh Token to get a new short-lived Access Token.
    @PostMapping("/renewAccessToken")
    public ResponseEntity<JwtResponseDto> reissueToken(@RequestBody RefreshTokenReissueRequestDto tokenRequest) {

        String refreshToken = tokenRequest.getRefreshToken();

        // Find, Validate Expiration, and Generate New Access Token
        String newAccessToken = refreshTokenService.findByToken(refreshToken) // Handles not found exceptions implicitly after the maps that happen if the token is found.
                .map(refreshTokenService::verifyExpiration) // Throw necessary Exception if expired or the user account is soft-deleted.
                .map(RefreshToken::getUser)                 // Get the User associated with the valid token
                .map(jwtService::generateAccessToken)       // Generate a new Access Token for that specified user
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token is not in database!")); // If refresh token not found or invalid

        // Return the new access token and the same valid refresh token
        return ResponseEntity.ok(
                JwtResponseDto.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken)
                        .build()
        );
    }
}