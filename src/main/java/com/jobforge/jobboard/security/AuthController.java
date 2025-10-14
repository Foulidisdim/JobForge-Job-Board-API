package com.jobforge.jobboard.security;

import com.jobforge.jobboard.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/renewAccessToken") //authentication-specific functionality
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;


    //Endpoint to renew an expired Access Token.
    // The client sends the long-lived Refresh Token to get a new short-lived Access Token.
    @PostMapping()
    public ResponseEntity<JwtResponseDto> reissueToken(@RequestBody RefreshTokenReissueRequestDto tokenRequest) {

        String refreshToken = tokenRequest.getRefreshToken();

        // Retrieve the token or throw InvalidTokenException if missing
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found!"));

        // Validate token (expiration and session invalidation)
        refreshTokenService.validate(token);

        // Generate new access token for the user that sent the refresh token
        String newAccessToken = jwtService.generateAccessToken(token.getUser().getEmail());

        // Return the new access token (refresh token remains the same)
        return ResponseEntity.ok(
                JwtResponseDto.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken)
                        .build()
        );
    }
}