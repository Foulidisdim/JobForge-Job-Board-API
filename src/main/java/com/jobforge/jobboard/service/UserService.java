package com.jobforge.jobboard.service;

import com.jobforge.jobboard.dto.*;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.EmailAlreadyInUseException;
import com.jobforge.jobboard.exception.EmailSoftDeletedException;
import com.jobforge.jobboard.exception.InvalidPasswordException;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import com.jobforge.jobboard.mapstructmapper.UserMapper;
import com.jobforge.jobboard.repository.UserRepository;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.security.JwtResponseDto;
import com.jobforge.jobboard.security.JwtService;
import com.jobforge.jobboard.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    // --Dependency injections--
    // The lombok requiredArgsConstructor annotation plus these effectively make the Dependency Injection.
    private final UserRepository userRepository;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

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
        // We need to handle soft deleted users (Say a deactivated account already exists with that email).
        String email = registrationDto.getEmail();

        //1. Check if user with that email already exists.
        if (userRepository.findByEmailAndDeletedFalse(email).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use: " + registrationDto.getEmail());
        }

        // 2. Check soft-deleted users.
        if (userRepository.findByEmailAndDeletedTrue(email).isPresent()) {
            // Instead of recovering, instruct the user to log in
            throw new EmailSoftDeletedException("An account with this email was previously deleted. Please log in to recover it, or continue signing up with a new account.");
        }

        // 3. Extract typed phone number from DTO, Sanitize & normalize it.
        String sanitizedPhone = sanitizeAndNormalizePhoneNumber(registrationDto.getPhoneNumber());
        registrationDto.setPhoneNumber(sanitizedPhone);

        // 4. Finally, create a new account.
        // Instead of multiple lines like user.setField(registrationDto.getField());, I use mapStruct to reduce boilerplate code.
        User user = userMapper.toEntity(registrationDto);

        // Plain Password is already valid from the pattern on the DTOs.
        // Hash the plain pass and set it to the user that will be saved in the DB.
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));

        User savedUser = userRepository.save(user); // returns the user with the DB initialized fields like id

        // AUTO-LOGIN after successful registration: Generate and return the tokens.
        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser).getToken();

        // 5. Return the dual token response (Access+Refresh)
        return JwtResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId())
                .userEmail(savedUser.getEmail())
                .build();
    }


    /// GET
    // User login.
    @Transactional()
    public JwtResponseDto login(UserLoginDto loginDto) {
        // Authenticate Credentials

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
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after authentication.")); // Should not happen.

        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(user);
        // Create/Update Refresh Token (Deletes old token, saves new one)
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        // If email and password match:
        //Auto-reactivate if the account was soft-deleted
        if(user.isDeleted()){
            user.setDeleted(false);
            userRepository.save(user);
        }

        // Return Dual Token Response (Access+Refresh)
        return JwtResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .userEmail(user.getEmail())
                .build();
    }

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

        // Delete refresh token to force re-login for safety.
        refreshTokenService.deleteTokenByUserId(principal.getId());
    }

    /// DELETE
    //SOFT delete
    @Transactional
    public void deleteUser(CustomUserDetails principal) {
        User user = findActiveUserById(principal.getId());

        // If the user is associated with a company, de-associate them
        // and set their role back to CANDIDATE. Do it to prevent a rare race condition
        // in which a user associated with a company is accessed moments before their deletion.
        if (user.getCompany() != null) {
            user.setCompany(null);
            user.setRole(Role.CANDIDATE);
        }

        // Now soft delete the user by setting the flag and save again.
        user.setDeleted(true);
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
