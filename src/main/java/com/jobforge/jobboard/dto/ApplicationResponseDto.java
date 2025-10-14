package com.jobforge.jobboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobforge.jobboard.enums.ApplicationStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class ApplicationResponseDto {
    private Long id;
    private String resumeUrl;
    private ApplicationStatus status;
    private String applicationNotes;
    private Instant appliedAt;
    private Instant updatedAt;

    // -- Nested DTOs --
    private JobResponseDto job;
    
    /// Candidate is OPTIONAL. Will NOT populate it when the candidate views their applications, for example.
    /// SETTING THE CANDIDATE TO NULL ON THE DTO MANUALLY IN A SERVICE METHOD WILL SKIP THE MAPPING WHEN RESPONDING TO THE CLIENT!
    /// Not doing so will preserve the candidate (e.g., needed when the recruiters view applications).
    // UPDATED THE MAPPER to optionally map this based on the service's request.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserResponseDto candidate;
    //-- Nested DTOs --
}