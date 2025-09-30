package com.jobforge.jobboard.exception;

// "Active or deactivated account is registered with this email"
public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}