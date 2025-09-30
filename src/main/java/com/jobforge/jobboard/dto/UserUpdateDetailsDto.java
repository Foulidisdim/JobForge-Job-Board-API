package com.jobforge.jobboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDetailsDto {

    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;

    @NotBlank // Required to have an email both for login and contact purposes.
    @Email
    private String email;

    private String phoneNumber;

    @Size(max = 500)
    private String profilePictureUrl;
}
