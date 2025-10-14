package com.jobforge.jobboard.security;

import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.exception.InvalidTokenException;
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
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    refreshTokenRepository.flush(); // Force delete to DB immediately
                });

        // Build the (new) token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString()) // Use a secure UUID for the token string
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .issuedAt(Instant.now())
                .user(user)
                .build();



        return refreshTokenRepository.save(refreshToken);
    }

    // VALIDATION (verify not expired AND expiry date is AFTER the last session invalidation time!)
    @Transactional
    public RefreshToken validate(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            // If expired, delete token from the DB and throw exception.
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token was expired. Please make a new sign in request");
        }

        Instant lastInvalidation = token.getUser().getSessionInvalidationTime();
        if (lastInvalidation != null && token.getIssuedAt().isBefore(lastInvalidation)) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token invalidated due to session invalidation action (logout, password change, or account deletion).");
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