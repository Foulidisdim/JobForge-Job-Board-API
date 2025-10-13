package com.jobforge.jobboard.security;

import com.jobforge.jobboard.entity.Company;
import com.jobforge.jobboard.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security's UserDetails interface.

 * Extend the standard UserDetails contract to include the user's
 * immutable Database ID (Long id). This ID is essential for fast, in-memory
 * authorization checks (e.g., using @PreAuthorize("#userId == principal.id"))
 * without requiring redundant database lookups!
 **/
public class CustomUserDetails implements UserDetails {

    //CUSTOM FIELDS
    /// -- GETTER -- Exposes the Database ID(Long) for secure, in-memory authorization checks
    @Getter
    private final Long id;
    /// -- GETTER -- Exposes the user's associated company for secure, in-memory authorization checks
    @Getter
    private final Company company;

    @Getter
    private final boolean isDeleted;

    @Getter
    private final Instant sessionInvalidationTime;

    // -- STANDARD CONTRACT FIELDS --

    // The user's login identifier (email).
    private final String email;

    // The hashed password stored in the database.
    private final String passwordHash;

    // The user's roles and permissions (e.g., ROLE_ADMIN, ROLE_CANDIDATE).
    private final Collection<? extends GrantedAuthority> authorities;

    // --CONSTRUCTOR --
    public CustomUserDetails(User user, Company company) {
        // Initialize fields using the data from the Hibernate User entity.
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();

        // Convert the User's Role Enum into a Collection of Spring Security authorities.
        // Spring expects roles prefixed with "ROLE_" (e.g., ROLE_CANDIDATE)
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()));

        this.id = user.getId();
        this.company = company; // TODO: Check in services if this does get the company with my current transactional implementation or requires JOIN FETCH user+company in the User repo. Probably need to check the UserDetailsImpl that sends the company for this.
        this.isDeleted = user.isDeleted();
        this.sessionInvalidationTime = user.getSessionInvalidationTime();
    }

    // -- CUSTOM GETTERS FOR AUTHORIZATION --

    // --- REQUIRED UserDetails IMPLEMENTATION --
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    ///Returns the user's roles. Used by Spring to enforce access rules (e.g., using @PreAuthorize("hasAuthority('ROLE_ADMIN')")).
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // --- Account Status Methods (Often default to true unless specific status management is needed) ---
    @Override
    public boolean isAccountNonExpired() {return true;}
    @Override
    public boolean isAccountNonLocked() {return true;}
    @Override
    public boolean isCredentialsNonExpired() {return true;}

    ///Prohibits soft deleted accounts from logging in. Not desirable because logging in to a soft deleted account is set to reactivate it. leave as default (return true).
    @Override
    public boolean isEnabled() {return true;}
}