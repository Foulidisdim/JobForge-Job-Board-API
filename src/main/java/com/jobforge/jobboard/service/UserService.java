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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    // --Dependency injections--
    private final UserRepository userRepository; //lombok requiredArgsConstructor plus this is the repository Dependency Injection

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private final AuthorizationService authorizationService;

    // TODO: Choose which data the admin will have administrative access in and implement the access. Also, Implement admin cleanups to also hard delete stuff (e.g. auto deletions after some days or immediate deletion of deactivated data functionality.

    /// CREATE
    // Signup a new user.
    // @Transactional ensures that a series of database operations are executed as a single, atomic unit.
    // If any part of the operation fails, the entire transaction is rolled back!!
    @Transactional
    public UserResponseDto signUp(UserRegistrationDto registrationDto) {
        // We need to handle soft deleted users (Say a deactivated account already exists with that email).
        String email = registrationDto.getEmail();

        //1. Check active users
        if (userRepository.findByEmailAndDeletedFalse(email).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use: " + registrationDto.getEmail());
        }

        // 2. Check soft-deleted users
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

        User saved = userRepository.save(user); // returns the user with the DB initialized fields like id
        return userMapper.toDto(saved); //Use the Dto to strip the db initialized and password fields when responding to the frontEnd
    }


    /// GET
    // User login.
    @Transactional()
    public UserResponseDto login(UserLoginDto loginDto) {
        User user =  userRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + loginDto.getEmail())); // In case the repo returns an optional object because the user wasn't found.

        // Validate password (For authentication)
        if(!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())){
            throw new InvalidPasswordException("Invalid password");
        }

        // If email and password match:
        //Auto-reactivate if the account was soft-deleted
        if(user.isDeleted()){
            user.setDeleted(false);
            userRepository.save(user);
        }
        return userMapper.toDto(user);
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
    public UserResponseDto updateUserDetails(Long userId, UserUpdateDetailsDto detailsDto, Long actorId) {
        User actor = findActiveUserById(actorId);
        authorizationService.ensureSelfOrAdmin(actor, userId);

        User user = findActiveUserById(userId);

        // Extract typed phone number from DTO, Sanitize & normalize it.
        String sanitizedPhone = sanitizeAndNormalizePhoneNumber(detailsDto.getPhoneNumber());
        detailsDto.setPhoneNumber(sanitizedPhone);

        userMapper.updateDetailsFromDto(detailsDto, user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void updatePassword(Long userId, UserUpdatePasswordDto passwordDto, Long actorId) {
        // Only the user can update the password. Not even the admin.
        authorizationService.ensureSelf(actorId, userId);

        User user = findActiveUserById(userId);

        // Validate the old password and enforce choosing a different new password
        if (!passwordEncoder.matches(passwordDto.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Old password is incorrect");
        } else if (passwordEncoder.matches(passwordDto.getNewPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("New password must be different from the old password");
        }

        user.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
    }

    /// DELETE
    //SOFT delete
    @Transactional
    public void deleteUser(Long userId, Long actorId) {
        User actor = findActiveUserById(actorId);
        authorizationService.ensureSelfOrAdmin(actor, userId);

        User user = findActiveUserById(userId);

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
