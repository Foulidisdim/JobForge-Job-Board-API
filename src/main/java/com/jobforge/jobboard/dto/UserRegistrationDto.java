package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class UserRegistrationDto {

    // No ID field present! DB assigns an auto-incrementing value.
    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;

    @Email
    @NotBlank
    @Size(max = 100)
    private String email;

    // Non-encrypted password received from the frontEnd.
    // Matches BCrypt character consideration limit
    // Will hash this before saving the user in the DB.
    @NotBlank
    @Size(min = 8, max = 72)
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{8,72}$",
            message = "Password must be between 8 and 72 characters and contain at least one uppercase letter, one digit, and one special character."
    )
    private String password;

    // Expects a concatenated string that includes the + sign AND the country code. Will be saved with no dashes or spaces.
    private String phoneNumber;

    @Size(max = 500)
    private String profilePictureUrl;
}