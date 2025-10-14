package com.jobforge.jobboard.enums;

public enum ApplicationStatus {

    // Initialized as APPLIED on creation.
    // Can be updated to UNDER_REVIEW (company is reviewing the application), REJECTED, or ACCEPTED (final company decision).
    // WITHDRAWN can only be set by the candidate.

    APPLIED,
    UNDER_REVIEW,
    REJECTED,
    ACCEPTED,
    WITHDRAWN
}