package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyCreationDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotBlank
    private String industry;

    @Size(max = 500)
    private String logoUrl;
}