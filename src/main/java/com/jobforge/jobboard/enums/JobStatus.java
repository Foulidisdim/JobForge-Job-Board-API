package com.jobforge.jobboard.enums;

public enum JobStatus {
    DRAFT, // A job that is created but posted.
    ACTIVE, // A publicly posted job that is accepting applications.
    CLOSED, // A job that is no longer accepting applications but the employer/recruiter can view.
    DELETED // A job that has been soft-deleted (for historical purposes).
}