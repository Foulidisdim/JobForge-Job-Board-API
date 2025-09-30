package com.jobforge.jobboard.exception;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ResourceNotFoundException.class,
            EmailAlreadyInUseException.class,
            EmailSoftDeletedException.class,
            InvalidPasswordException.class,
            DuplicateResourceException.class,
            RepostLimitExceededException.class,
            UnauthorizedException.class,

            // Standard Java exceptions handled to standardize responses for the frontend.
            IllegalArgumentException.class,
            IllegalStateException.class
    })

    public ResponseEntity<ErrorResponse> handleCustomExceptions(RuntimeException ex) {

        HttpStatus status;

        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (
                ex instanceof EmailAlreadyInUseException ||
                ex instanceof EmailSoftDeletedException ||
                ex instanceof DuplicateResourceException
                  )
        {
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof InvalidPasswordException) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof IllegalStateException || ex instanceof UnauthorizedException) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof  RepostLimitExceededException) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR; //fallback
        }

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    // Special handler for polishing validation errors. Standardized output message of format "lastName: must not be blank; email: must be a valid email".
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Collect all field error messages (defaultMessage field from the error)
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); //Standardized error response with the code, message and timestamp like all the above!
    }

    @ExceptionHandler(org.apache.coyote.BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}