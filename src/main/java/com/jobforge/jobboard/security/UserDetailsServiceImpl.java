package com.jobforge.jobboard.security;

import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Role of the PASSWORD HASH in UserDetails:

 * 1. IS USED: Initial Login (Password Authentication):
 * - The password hash IS USED by the DaoAuthenticationProvider (managed by the AuthenticationManager)
 * to compare the plain-text password sent by the client against the hash loaded from the DB.

 * 2. ISN'T USED: Token-Based Access on secured controller methods (JWT Authentication - JwtAuthenticationFilter):
 * - The hash ISN'T USED by the JwtAuthenticationFilter.
 * - JWT validation replaces password validation. We pass 'null' for credentials when creating the token.

 * However: The UserDetailsService MUST satisfy its UserDetailsService interface contract
 * ("Given a username (email), return everything Spring needs to know about this user (email, hashed password, roles")
 * to serve as a universal component for ALL authentication forms, so the hash must be loaded
 * regardless of whether the JWT filter ignores it.
 * */

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Loads the user data required by Spring Security for authentication. 'Username' in our case is the user's email.
    @Override
    public CustomUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Find the user entity by email (and ensure they are not deleted)
        User user = userRepository.findByEmailAndDeletedFalse(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Return my CUSTOMuserDetails implementation with id and company
        return new CustomUserDetails(user, user.getCompany());
    }
}
