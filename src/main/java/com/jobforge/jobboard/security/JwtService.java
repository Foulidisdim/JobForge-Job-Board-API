package com.jobforge.jobboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userEmail) {
        // Using the modern java 8+ Instant for the Date. JWT still expects Date, so I convert when passing on the JWT builder.
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessExpirationMs);

        return Jwts.builder()
                .setSubject(userEmail) // "Subject" is the user's login identifier (email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Signs token header and payload to ensure authenticity. Uses the secret key from the environment variable (if present) or the specified secret key in application.yml.
                .compact();
    }

    // Extracts the received token's payload (claims: issuedAt, expiry, userId)
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token) // The Signature is verified here.
                .getBody(); // Extracts into a map of type Claims.
    }

    // Verify token is not expired and is associated with the currently authenticated user.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);

            String username = claims.getSubject();
            Date expiration = claims.getExpiration();

            return username.equals(userDetails.getUsername()) && !expiration.before(Date.from(Instant.now()));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Performs the core revocation check (security feature).
     * @param tokenIssuedAt The time the token was issued (from the 'iat' claim).
     * @param lastSessionInvalidationTime The time the user last logged out (from the User entity).
     * @return true if the token was issued BEFORE or AT the last logout time, meaning it's revoked.
     */
    public boolean isTokenRevoked(Instant tokenIssuedAt, Instant lastSessionInvalidationTime) {
        if (lastSessionInvalidationTime == null) {
            return false;
        }
        if (tokenIssuedAt == null) {
            return true; // Malformed token without iat should be rejected
        }
        // If token issued before logout time -> invalid.
        return !tokenIssuedAt.isAfter(lastSessionInvalidationTime); // Careful. True if the token is newer than invalidation, so with !, we return FALSE (not revoked).
    }
}