package com.jobforge.jobboard.dto;

import com.jobforge.jobboard.enums.EmploymentType;
import com.jobforge.jobboard.enums.ExperienceLevel;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.enums.WorkArrangement;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class JobResponseDto {

    private Long id;
    private String title;
    private String location;
    private String description;
    private EmploymentType employmentType;
    private ExperienceLevel experienceLevel;
    private WorkArrangement workArrangement;
    private Double salaryMin;
    private Double salaryMax;
    private String currencyCode;
    private Instant updatedAt;
    private Instant repostedAt;


    // Nested DTO for the company. Required for frontend display of the associated company.
    // Company data response mapping occurs in JobMapper.
    private CompanyResponseDto company;

    // Nested DTO list for required skills; needed for frontend display.
    // Skill data response mapping occurs in JobMapper.
    private List<SkillResponseDto> skills;

    private JobStatus status;
}