package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRecoveryCompletionDto {
    @NotBlank
    private String recoveryToken;

    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{8,72}$",
            message = "Password must be between 8 and 72 characters and contain at least one uppercase letter, one digit, and one special character."
    )
    private String newPassword;
}