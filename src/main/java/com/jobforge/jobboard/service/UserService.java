package com.jobforge.jobboard.service;

import com.jobforge.jobboard.dto.*;
import com.jobforge.jobboard.entity.Company;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.*;
import com.jobforge.jobboard.mapstructmapper.CompanyMapper;
import com.jobforge.jobboard.mapstructmapper.UserMapper;
import com.jobforge.jobboard.repository.UserRepository;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.security.JwtResponseDto;
import com.jobforge.jobboard.security.JwtService;
import com.jobforge.jobboard.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    // --Dependency injections--
    // The lombok requiredArgsConstructor annotation plus these effectively make the Dependency Injection.
    private final UserRepository userRepository;

    private final CompanyMapper  companyMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final BusinessRuleService businessRuleService;

    private final ApplicationService applicationService;
    private final JobService jobService;
    private final CompanyService companyService;


    // TODO: Choose which data the admin will have administrative access in (e.g update or delete other user's data) and implement the access. Also, Implement admin cleanups to also hard delete stuff (e.g. auto deletions after some days or immediate deletion of deactivated data functionality.

    /// BY DESIGN (in ALL services):
    /// In SecurityConfig: signUp, login and the /auth/refresh token refresh endpoints are publicly exposed. All other exposed functionalities NEED an authenticated user.
    /// IF NO @PreAuthorize CONSTRAINTS ARE IN PLACE: Service methods receive the authenticated principal from the controllers, meaning that a USER can ONLY CHANGE THEIR OWN DETAILS!


    /// CREATE
    // Signup a new user.
    // @Transactional ensures that a series of database operations are executed as a single, atomic unit.
    // If any part of the operation fails, the entire transaction is rolled back!!
    @Transactional
    public JwtResponseDto signUp(UserRegistrationDto registrationDto) {
        // Check if user with that email already exists/is soft-deleted.
        Optional<User> existingUser = userRepository.findByEmail(registrationDto.getEmail());
        String email = registrationDto.getEmail(); // Assuming this is defined earlier

        if (existingUser.isPresent()) {
            User user = existingUser.get(); // Correctly retrieve the User object

            if (user.isDeleted()) {
                throw new EmailSoftDeletedException("An account with this email was previously deleted. Please try recovering it, or continue signing up with a new account.");
            }
            // If account exists & NOT deleted -> it's currently in use
            throw new EmailAlreadyInUseException("Email already in use: " + email);
        }

        // Extract typed phone number from DTO, Sanitize & normalize it.
        String sanitizedPhone = sanitizeAndNormalizePhoneNumber(registrationDto.getPhoneNumber());
        registrationDto.setPhoneNumber(sanitizedPhone);

        // Finally, create a new account.
        // Instead of multiple lines like user.setField(registrationDto.getField());, I use mapStruct to reduce boilerplate code.
        User user = userMapper.toEntity(registrationDto);

        // Plain Password is already valid from the pattern on the DTOs.
        // Hash the plain pass and set it to the user that will be saved in the DB.
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));

        User savedUser = userRepository.save(user); // returns the user with the DB initialized fields like id

        // AUTO-LOGIN after successful registration: Generate and return the tokens.
        String accessToken = jwtService.generateAccessToken(savedUser.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(savedUser).getToken();

        // 5. Return the dual token response (Access+Refresh)
        return JwtResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId())
                .userEmail(savedUser.getEmail())
                .build();
    }

    // User login.
    @Transactional()
    public JwtResponseDto login(UserLoginDto loginDto) {
        /// AuthenticationManager: Automatic UserDetailsServiceImpl and PasswordEncoder Dependencies!:
        /*
          Spring Security automatically delegates the authentication request to the
          DaoAuthenticationProvider, which is automatically wired with:

          1. UserDetailsService: (@Service UserDetailsServiceImpl I created)
          To load the UserDetails object (including the hashed password)
          from the db based on email.

          2. PasswordEncoder: (@Bean PasswordEncoder where I configured with BCrypt in SecurityConfig)
          To securely compare the plain-text password from the request
          against the hashed password retrieved from the DB.

          For subsequent secured requests, the JwtAuthenticationFilter BYPASSES the AuthenticationManager,
          validates the token directly, and manually sets the user's authentication context in Sping Security Context.
        */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );

        // Load the authenticated User entity from the DB using the authenticated user's details
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        // 2. Check for soft-deletion
        if (customUserDetails.isDeleted()) {
            // DisabledException is perfect for logically disabled accounts
            throw new DisabledException("Account deactivated. Please recover your account before logging in.");
        }


        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(loginDto.getEmail());
        // Create/Update Refresh Token (Deletes old token, saves new one)
        User user = findActiveUserById(customUserDetails.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        // Return Dual Token Response (Access+Refresh)
        return JwtResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(customUserDetails.getId())
                .userEmail(customUserDetails.getUsername())
                .build();
    }

    @Transactional
    public void logout(Long principalId) {
        User user = findActiveUserById(principalId);
        businessRuleService.invalidateAllAuthenticationTokens(user);
    }

    /// The "forgot password" flow. FrontEnd can lead to this in case of a password reset request, OR
    /// if a deactivated account was found during log in or sign in (proves account and email ownership and control, and sets a new password for safety).
    @Transactional
    public String initiateRecovery(String email) {

        // Check if the account exists and is soft-deleted
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email."));

        if (!user.isDeleted()) {
            throw new IllegalStateException("This account is already active. You can log in with your credentials.");
        }

        // Generate and save the token (20 minutes validity)
        String recoveryToken = UUID.randomUUID().toString();
        user.setRecoveryToken(recoveryToken);
        user.setRecoveryTokenExpirationTime(Instant.now().plusSeconds(20 * 60)); // 20 minutes

        // TODO: In production, replace the return with an email service call. E.g., emailService.sendRecoveryLink(user.getEmail(), recoveryToken);
        return recoveryToken; // Temporarily return the token for testing
    }

    @Transactional
    public void completeRecovery(String token, String newPassword) {

        User user = userRepository.findByRecoveryToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid recovery token."));

        if (user.getRecoveryTokenExpirationTime() == null || user.getRecoveryTokenExpirationTime().isBefore(Instant.now())) {
            throw new InvalidTokenException("Recovery token has expired.");
        }

        // Check for deleted status (Logically impossible to fail but kept for defensive coding)
        if (!user.isDeleted()) {
            throw new IllegalStateException("Account is already active. Recovery is not needed.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalStateException("The new password cannot be the same as the previous password.");
        }

        // Update password and reactivate account
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setDeleted(false);

        businessRuleService.invalidateAllAuthenticationTokens(user);

        // 4. Clean up token fields
        user.setRecoveryToken(null);
        user.setRecoveryTokenExpirationTime(null);
    }


    /// GET
    @Transactional(readOnly = true)
    public List<UserResponseDto> findAllActiveUsers() {
        return userRepository.findAllByDeletedFalse()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getActiveUserDtoById(Long userId) {
        User user = findActiveUserById(userId);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public CompanyResponseDto getCompanyForUser(Long actorId) {

        User user = findActiveUserById(actorId);

        // The user must be associated with a company.
        if (user.getCompany() == null) {
            throw new IllegalStateException("User is not associated with any company.");
        }

        return companyMapper.toDto(user.getCompany());
    }


    /// UPDATE
    @Transactional
    public UserResponseDto updateUserDetails(UserUpdateDetailsDto detailsDto, CustomUserDetails principal) {

        // Received authenticated user details from security context from the controller.
        /// BY DESIGN: The authenticated user can only change their own details!
        User user = findActiveUserById(principal.getId());

        // Extract typed phone number from DTO, Sanitize & normalize it.
        String sanitizedPhone = sanitizeAndNormalizePhoneNumber(detailsDto.getPhoneNumber());
        detailsDto.setPhoneNumber(sanitizedPhone);

        userMapper.updateDetailsFromDto(detailsDto, user);
        return userMapper.toDto(user);
    }

    @Transactional
     // Only the authenticated user can update (their own only) password.
    public void updatePassword(UserUpdatePasswordDto passwordDto, CustomUserDetails principal) {
        User user = findActiveUserById(principal.getId());

        // Validate the old password and enforce choosing a different new password
        if (!passwordEncoder.matches(passwordDto.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Old password is incorrect");
        } else if (passwordEncoder.matches(passwordDto.getNewPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("New password must be different from the old password");
        }

        user.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));

        businessRuleService.invalidateAllAuthenticationTokens(user);
    }

    /// DELETE
    //SOFT delete
    @Transactional
    public void deleteUser(CustomUserDetails principal) {
        User user = findActiveUserById(principal.getId());
        Long userId = user.getId();
        Company company = user.getCompany();

        // APPLICATIONS HANDLING
        applicationService.handleUserAccountDeletionApplicationsCleanup(userId);

        // COMPANY & JOB INTEGRITY CHECK (upon employer/recruiter deletion)
        if (company != null) {

            // If the deleted user was the company's Employer, the company structure is broken.
            if (user.getRole() == Role.EMPLOYER) {
                // The company itself and its jobs must be soft-deleted. All company recruiters must also become candidates.
                companyService.handleCompanySoftDeletionBecauseOfEmployerDeletion(company);
            }
            else if (user.getRole() == Role.RECRUITER) {
                User currentEmployer = company.getEmployer();
                // Call the job transfer directly, as the company remains active and has a manager.
                jobService.transferJobManagement(userId, company.getId(), currentEmployer);
            }

            // Necessary disassociations
            company.getRelatedUsers().remove(user);
            user.setCompany(null);
            user.setRole(Role.CANDIDATE); // Best to revert to base role before soft deletion
        }

        // Now soft delete the user by setting the flag and revoke access.
        user.setDeleted(true);
        businessRuleService.invalidateAllAuthenticationTokens(user);
    }


    /// Internal service-to-service data exchange methods (separation of concerns)
    @Transactional(readOnly = true)
    public User findActiveUserById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }



    /// -- Helper methods --
    private static String sanitizeAndNormalizePhoneNumber(String phone) {
        if (phone != null && !phone.isBlank()) {
            // Remove spaces, dashes, and parentheses
            String sanitized = phone.replaceAll("[\\s\\-()]", "");

            // Validate format: 7–15 digits, may start with + or 00
            if (!sanitized.matches("^(00|\\+)?\\d{7,15}$")) {
                throw new IllegalArgumentException("Invalid phone number format.");
            }

            // Normalize to + format if starts with 00
            if (!sanitized.startsWith("+")) {
                sanitized = "+" + sanitized.replaceAll("^00", "");
            }

            return sanitized;
        }
        return null;
    }
}