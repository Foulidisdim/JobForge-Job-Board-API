package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompanyResponseDto {

    @NotNull
    private Long id;

    @NotBlank
    private String name;

    private String description;

    private String logoUrl;

    @NotBlank
    private String industry;
}