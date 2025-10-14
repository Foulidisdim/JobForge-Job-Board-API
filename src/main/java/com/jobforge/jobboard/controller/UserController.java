package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.*;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.security.JwtResponseDto;
import com.jobforge.jobboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users") //will be the "base URL" for all endpoints of the user controller.
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // TODO: Use the "findbyid" method for internal use only after my tests. Public search of data should happen with other identifiers like Name (users) or title (job).
    // TODO: Remember to @Valid on ALL @RequestBody parameters on the controllers!

    /// CREATE
    @PostMapping("/signup")
    public ResponseEntity<JwtResponseDto> signUp (@Valid @RequestBody UserRegistrationDto registrationDto) {  //@Valid: validate the input as defined on the userCreateDto annotations

        JwtResponseDto tokensResponse = userService.signUp(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokensResponse);
    }
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> login (@Valid @RequestBody UserLoginDto loginDto) {
        JwtResponseDto tokensResponse = userService.login(loginDto);
        return ResponseEntity.ok(tokensResponse);
    }

    //Logs out the current user. Deletes refresh tokens and updates the user's session invalidation timestamp
    // to immediately invalidate any still-active access tokens (prevents malicious use during the token's
    // remaining lifetime, e.g., 15 minutes).
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails principal) {
            userService.logout(principal.getId());
            return ResponseEntity.ok("Logged out successfully.");
    }

    // TODO: Edit this after email recovery is implemented (return 200 OK with a simple message).
    @PostMapping("/recover/initiate")
    public ResponseEntity<String> initiateAccountRecovery(@Valid @RequestBody UserRecoveryInitiationDto recoveryInitiationDtoDto) {
        String recoveryToken = userService.initiateRecovery(recoveryInitiationDtoDto.getEmail());
        return ResponseEntity.ok(recoveryToken);
    }

    @PostMapping("/recover/complete")
    public ResponseEntity<String> completeRecovery(@Valid @RequestBody UserRecoveryCompletionDto recoveryCompletionDtoDto) {
        userService.completeRecovery(recoveryCompletionDtoDto.getRecoveryToken(), recoveryCompletionDtoDto.getNewPassword());
        return ResponseEntity.ok("Account successfully recovered. You can now log in.");
    }


    /// READ
    // Active users only
    @GetMapping()
    public ResponseEntity<List<UserResponseDto>> getAllActiveUsers() {

        List<UserResponseDto> userDtos = userService.findAllActiveUsers();
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}") // GET request for a specific user I need
    public ResponseEntity<UserResponseDto> getActiveUserById(@PathVariable Long id) { //@PathVariable copies the id from the {id} specified on the URL

        UserResponseDto userResponseDto = userService.getActiveUserDtoById(id);
        return ResponseEntity.ok(userResponseDto); //returns the user or throws the not found exception
    }

    @GetMapping("/{id}/company")
    public ResponseEntity<CompanyResponseDto> getCompanyForUser(@PathVariable Long id) {

        CompanyResponseDto company = userService.getCompanyForUser(id);
        return ResponseEntity.ok(company);
    }


    /// UPDATE
    // Active users only
    @PutMapping()
    public ResponseEntity<UserResponseDto> updateActiveUser(@Valid @RequestBody UserUpdateDetailsDto detailsDto, @AuthenticationPrincipal CustomUserDetails principal) {

        UserResponseDto updatedUser = userService.updateUserDetails(detailsDto, principal);
        return ResponseEntity.ok(updatedUser);

    }

    @PatchMapping("/changePassword") // @PatchMapping declares it is a partial update
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UserUpdatePasswordDto passwordDto, @AuthenticationPrincipal CustomUserDetails principal) {
        userService.updatePassword(passwordDto, principal);
        return ResponseEntity.noContent().build();
    }


    /// DELETE
    @DeleteMapping()
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails principal) {
        userService.deleteUser(principal);
        return ResponseEntity.noContent().build();
    }
}