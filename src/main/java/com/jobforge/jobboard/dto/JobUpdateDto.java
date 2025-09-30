package com.jobforge.jobboard.dto;

import com.jobforge.jobboard.customValidation.ValidCurrency;
import com.jobforge.jobboard.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

// TODO: Make All update DTO fields optional.
@Data
public class JobUpdateDto {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String location;

    @NotBlank
    private String description;

    private EmploymentType employmentType;
    private ExperienceLevel experienceLevel;
    private WorkArrangement workArrangement;

    @NotNull
    @PositiveOrZero
    private Double salaryMin;

    @NotNull
    @PositiveOrZero
    private Double salaryMax;

    @NotBlank
    @Size(min = 3, max = 3)
    @ValidCurrency
    private String currencyCode;

    @NotEmpty
    private List<Long> skillIds;

    @NotNull
    @EnumSubset(enumClass = JobStatus.class, anyOf = {"DRAFT", "ACTIVE"}) // Custom enum validator!
    private JobStatus status;
}
