package com.jobforge.jobboard.security;

import com.jobforge.jobboard.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// @Builder:
// Useful for a readable and explicit way to create RefreshToken instances
// with only some fields initialized (We don't set the auto-incremented id).
// If we used AllArgsConstructor, we should have written null,-other fields here- to ignore the id.
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /* The token string itself (the LONG, SECURE KEY stored as a String).

       - Key Generation: The token is created as a 128-bit UUID (a random,
         virtually one-of-a-kind identifier) using `UUID.randomUUID().toString()` in the RefreshTokenService.

       - This UUID acts as the token itself, and as a unique and traceable name tag for database lookup ("IDENTIFIER").
       - The randomness of UUID provides refreshToken identifiers that have an astronomically low probability
         of being duplicated or guessed.

       - Security Rationale: Storing and transmitting this long, random string
         makes brute-force attacks that try to guess the refresh token computationally infeasible.
    */
    @Column(nullable = false, unique = true)
    private String token;


    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private Instant issuedAt;

    /**
     * SECURITY NOTE: We DO NOT define a 'back-reference' ('private RefreshToken refreshToken;')
     * on the User entity itself. This prevents accidentally loading or exposing
     * a user's refresh token details through the User object.
     * Tokens are managed only via the RefreshTokenRepository and Service.
     **/
    // The user this token belongs to! DON'T DEFINE REL. ON THE USER!
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}