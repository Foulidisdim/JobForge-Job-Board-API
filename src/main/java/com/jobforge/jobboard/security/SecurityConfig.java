package com.jobforge.jobboard.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final PublicPaths publicPaths;

    // PasswordEncoder is an interface, and I use the BCrypt bean I defined to define its implementation!
    // With @Bean, spring can autowire the dependency and I HAVE THE ADVANTAGE OF FLEXIBILITY
    // because I can easily just change the implementation defined to a new encoder!!
    // I could, for example, use Argon2 for hashing and my service would stay exactly the same.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // Configures how some basic Spring Security filter chain filters work. The filters
    // Intercept all incoming requests and ensure security before they reach the controllers.
    // We add OUR CUSTOM JWT FILTER AT THE END!
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Enable CORS and allow the filter to be applied.
            .cors(Customizer.withDefaults())

            // 2. Disable CSRF protection (attacks where the browser sends malicious cookies).
            //    Using JWT means the API is STATELESS:
            //      - No session sessions or cookies saved in the server because
            //      - Each secured request from the frontEnd WILL INCLUDE THE JWT TOKEN in the authorization header.
            .csrf(AbstractHttpConfigurer::disable)

            // 3. Configure Spring security's 403 (authorization failure) and
            // 401 (authentication failure) response to be more descriptive with my custom errorResponse format! (message+timestamp too)
            .exceptionHandling(exception ->
                    exception
                            .authenticationEntryPoint(customAuthenticationEntryPoint)
                            .accessDeniedHandler(customAccessDeniedHandler)
            )

            // 4. Configure authorization for requests.
            .authorizeHttpRequests(authorize -> authorize
                    // Allow public access to these endpoints (.permitAll()).
                    .requestMatchers(publicPaths.getPublicPaths().toArray(String[]::new)).permitAll()
                    // All other requests MUST be authenticated (.authenticated())
                    .anyRequest().authenticated()
            )

            // 5. Configure Session Management (For JWT. "Don't create/store HTTP sessions. Expect a JWT token for authentication")
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))


            /// 6. Add the JWT Filter (For JWT)
            /// Run the custom auth filter BEFORE Spring's standard authentication process
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // Defines the CORS configuration for frontend-backend communication. Again, @Bean required for the configuration to be autowired.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Replace this with the domain of the frontend application.
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // Allowed all HTTP methods and headers, and allow credentials (cookies, authorization headers, etc.).
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    // Authentication Manager Bean (Needed by the UserService for login)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Spring Boot already provides the implementation via AuthenticationConfiguration
        return config.getAuthenticationManager();
    }
}