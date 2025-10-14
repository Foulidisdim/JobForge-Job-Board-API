package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationCreationDto {

    //No need for the frontEnd to explicitly give the associated candidate's ID for the application!
    // The ID will be available in the service layer from the JWT token of the logged-in User.

    @NotNull // A Long value either be a valid number or null. No such thing as whitespace like in strings. @NotNull is enough to ensure some kind of value.
    private Long jobId; // Frontend will send the job ID the candidate is applying to.

    @NotBlank
    private String resumeUrl;

}