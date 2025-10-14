package com.jobforge.jobboard.dto;

import com.jobforge.jobboard.enums.ApplicationStatus;
import com.jobforge.jobboard.enums.EnumSubset;
import com.jobforge.jobboard.enums.JobStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationUpdateDto {

    // DTO for updating an application (employer/recruiter use only).
    // Service logic automatically handles ACTIVE and UNDER_REVIEW statuses; company only sets ACCEPTED or REJECTED to respond to the candidate.
    // WITHDRAWN can only be set by the candidate.
    // Fields are optional, so no @NotNull annotations are used.

    @EnumSubset(enumClass = JobStatus.class, anyOf = {"REJECTED", "ACCEPTED"})
    private ApplicationStatus status;

    @NotBlank
    private String applicationNotes;
}