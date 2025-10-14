package com.jobforge.jobboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobforge.jobboard.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // Set the status to 403 Forbidden
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Create your standardized error response body
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access Denied: You do not have the necessary permissions (role) to perform this action.",
                LocalDateTime.now()
        );

        // Write the error response to the output stream
        OutputStream out = response.getOutputStream();
        objectMapper.writeValue(out, errorResponse);
        out.flush();
    }
}