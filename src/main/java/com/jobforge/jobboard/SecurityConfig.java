package com.jobforge.jobboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // PasswordEncoder is an interface, and I use the BCrypt bean I defined to define its implementation!
    // With @Bean, spring can autowire the dependency and I HAVE THE ADVANTAGE OF FLEXIBILITY
    // because I can easily just change the implementation defined to a new encoder!!
    // I could, for example, use Argon2 for hashing and my service would stay exactly the same.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configures the main Spring Security filter chain.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Enable CORS and allow the filter to be applied.
                .cors(Customizer.withDefaults())
                // 2. Disable CSRF for testing.
                .csrf(AbstractHttpConfigurer::disable)
                // 3. Configure authorization for requests.
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll() // Allow all requests for now
                );
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
}