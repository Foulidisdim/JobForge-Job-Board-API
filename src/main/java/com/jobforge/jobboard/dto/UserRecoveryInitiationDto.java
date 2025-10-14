package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRecoveryInitiationDto {
    @Email
    @NotBlank
    @Size(max = 100)
    private String email;
}