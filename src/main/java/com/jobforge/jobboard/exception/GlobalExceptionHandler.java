package com.jobforge.jobboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // -- Validation Errors --
    /// Special handler for polishing validation errors from @Valid annotated DTOs.
    /// Standardized output message of format "lastName: must not be blank; email: must be a valid email".
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }


    /// -- Authentication & Authorization --
    // Handles Spring Security's BadCredentialsException when trying to log in with incorrect email or password. Provides a more user-friendly message.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }


    /// -- Resource & Business Logic Exceptions --
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({EmailAlreadyInUseException.class, EmailSoftDeletedException.class, DuplicateResourceException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(InvalidPasswordException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RepostLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(RepostLimitExceededException ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }


    /// -- Catch-all fallback for unexpected errors --
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }


    /// -- Helper method --
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}