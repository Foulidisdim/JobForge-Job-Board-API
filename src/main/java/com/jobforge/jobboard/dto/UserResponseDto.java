package com.jobforge.jobboard.dto;

import com.jobforge.jobboard.enums.Role;
import lombok.Data;

@Data
public class UserResponseDto {

    // Generic DTO for returning all user information.
    // Password hash is intentionally excluded for security.
    // A DTO's job is to customize fields per use case to enforce safety and privacy.

    // Unique identifier included in the response for client-side reference,
    // used for updates or deletions on the database.
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private Role role;
}
