package com.jobforge.jobboard.security;

import com.jobforge.jobboard.entity.User;
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

    public String generateAccessToken(User user) {
        // Using the modern java 8+ Instant for the Date. JWT still expects Date, so I convert when passing on the JWT builder.
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessExpirationMs);

        return Jwts.builder()
                .setSubject(user.getEmail()) // "Subject" is the user's login identifier (email)

                    // Apart from subject, issuedAt and Expiration claims,
                    // ALSO Embed the user's DB UNIQUE IDENTIFIER (id)
                    // directly into the token payload so that the API services can fetch user info without a DB lookup!
                    .claim("uid", user.getId())
                    .claim("deleted",user.isDeleted())

                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Signs token header and payload to ensure authenticity.// Uses the secret key from the environment variable (if present) or the specified key in application.yml.
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

    public String extractUsername(String token) {
        // We get the subject (email) from the claims body
        return parseClaims(token).getSubject();
    }

    public boolean extractIsDeletedUserStatus(String token) {
        return parseClaims(token).get("deleted", Boolean.class);
    }
}
