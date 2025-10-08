package com.jobforge.jobboard.security;

import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs; // Non-hard-coded application.yml configuration regulating when RefreshTokens expire.

    private final RefreshTokenRepository refreshTokenRepository;

    // CREATION
    /// Happens on 1. EVERY LOGIN or 2. WHEN REFRESH TOKEN EXPIRES
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Delete any old refresh tokens for this user (one active token per user is best practice)
        refreshTokenRepository.findByUserId(user.getId()).ifPresent(refreshTokenRepository::delete);

        // Build the (new) token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString()) // Use a secure UUID for the token string
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .user(user)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // VALIDATION (verify not expired and account not soft deleted)
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            // If expired, delete token from the DB and throw exception.
            refreshTokenRepository.delete(token);
            throw new ResourceNotFoundException("Refresh token was expired. Please make a new sign in request");
        }

        if (token.getUser().isDeleted()) {
            // If user account is soft-deleted, delete token from the DB and throw exception.
            refreshTokenRepository.delete(token);
            throw new SecurityException("Account is deactivated.");
        }

        return token;
    }

    // RETRIEVAL
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // DELETION
    @Transactional
    public void deleteTokenByUserId(Long userId) {
        refreshTokenRepository.findByUserId(userId).ifPresent(refreshTokenRepository::delete);
    }
}
