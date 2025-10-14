package com.jobforge.jobboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.jobboard.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // Set the status to 401 Unauthorized
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Create your standardized error response body
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed: Invalid or missing JWT token.",
                LocalDateTime.now()
        );

        // Write the error response to the output stream
        OutputStream out = response.getOutputStream();
        objectMapper.writeValue(out, errorResponse);
        out.flush();
    }
}