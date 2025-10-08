package com.jobforge.jobboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// Spring does not natively handle JWT verification; it handles sessions.
/// This is my custom JWT solution, intercepting every HTTP request that needs authentication.

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Ensures filtering happens only once per HTTP request.

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    // Override OncePerRequestFilter's official entry point for custom logic for JWT authentication.
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, //THE WHOLE HTTP request from the client
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Reads the authorization header of the request, where the JWT is in.
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check if the request is trying to authenticate OR NOT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No JWT found, so just pass the request to the next filter in the chain.
            // When it passes through all filters, it reaches the GET (or POST respectively) controller method.
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the RAW JWT STRING (TOKEN PRESENT. We didn't enter the above if Statement)
        jwt = authHeader.substring(7); // "Bearer " (with that space) is 7 characters long. The JWT Token string follows.

        // 3. Extract the username (email) from the token
        userEmail = jwtService.extractUsername(jwt);

        // 4. Validate the user and token
        // Check if the userEmail is valid AND if the user is not currently authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load UserDetails (loads user roles and password hash FROM THE UserDetailsServiceImpl we defined!!)
            CustomUserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Check if the token is valid (not expired, signature matches, etc.)
            if (jwtService.isTokenValid(jwt, userDetails)) {

                /// EDGE CASE CHECK: User account deleted but tokens still active! REFUSE API ACCESS TILL ACCOUNT RECOVERY!
                boolean isDeleted = jwtService.extractIsDeletedUserStatus(jwt);
                if (isDeleted) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Account is deactivated. Access denied.");
                    response.flushBuffer(); // Exit authentication. Don't pass down to the filter chain!
                    return;
                }

                // 5. Create Authentication Token
                // If valid, create an authentication object for Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(

                        userDetails,
                        null, //// Credentials null because the JWT ITSELF IS THE CREDENTIAL! No password is needed. We've validated the token, not a password.
                        userDetails.getAuthorities() // Load a COLLECTION OF the user's ROLES (e.g., ROLE_CANDIDATE) to enforce them in possible security checks!!
                        /// Once the user is authenticated, Spring Security uses these authorities to
                        /// manage access (E.g, with @PreAuthorize("hasRole('ADMIN')")
                );

                // Set authentication details (remote IP, session ID, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                /// 6. Update Security Context
                /// Set the authentication token in Spring's SecurityContextHolder
                /// Spring Security considers the user fully logged in and authorized for this request.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 7. Pass to Next Filter
        // Continue processing the request down the chain (or to the controller if all filters are done)
        // Parameters:
        // 1. Updated client request (The original, that also contained the Security Context, WHICH WE UPDATED in this method!),
        //
        // 2. HTTP response that is now being built. Filters can use it to set headers/cookies before the request reaches the controller to finalize the response OR
        //    short-circuit the request (Immediately send an HTTP 500 error to the client before the request reaches the controller, if breach detected, using response.flushBuffer()).
        filterChain.doFilter(request, response);

    }
}

