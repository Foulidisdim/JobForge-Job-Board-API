package com.jobforge.jobboard.exception;

// "Active or deactivated account is registered with this email"
public class EmailSoftDeletedException extends RuntimeException {
    public EmailSoftDeletedException(String message) {
        super(message);
    }
}