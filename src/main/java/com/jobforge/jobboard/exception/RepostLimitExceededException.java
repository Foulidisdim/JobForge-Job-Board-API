package com.jobforge.jobboard.exception;

public class RepostLimitExceededException extends RuntimeException {
    public RepostLimitExceededException(String message) {
        super(message);
    }
}
