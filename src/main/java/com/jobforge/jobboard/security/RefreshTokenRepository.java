package com.jobforge.jobboard.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    // To find and delete the existing refresh token for a user when creating a new refresh token.
    Optional<RefreshToken> findByUserId(Long userId);
}
