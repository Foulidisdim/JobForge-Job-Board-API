package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.*;
import com.jobforge.jobboard.service.CompanyService;
import com.jobforge.jobboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users") //will be the "base URL" for all endpoints of the user controller.
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CompanyService companyService;

    // TODO: Use the "findbyid" method for internal use only after my tests. Public search of data should happen with other identifiers like Name (users) or title (job).
    // TODO: Remember to @Valid on ALL @RequestBody parameters on the controllers!

    /// CREATE
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signUp (@Valid @RequestBody UserRegistrationDto registrationDto) {  //@Valid: validate the input as defined on the userCreateDto annotations

        UserResponseDto userResponse = userService.signUp(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }
    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login (@Valid @RequestBody UserLoginDto loginDto) {

        UserResponseDto userResponse = userService.login(loginDto);
        return ResponseEntity.ok(userResponse);
    }


    /// READ
    // Active users only
    @GetMapping("/all")
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

        CompanyResponseDto company = companyService.getCompanyForUser(id);
        return ResponseEntity.ok(company);
    }


    /// UPDATE
    // Active users only
    @PutMapping("/{id}/details")
    public ResponseEntity<UserResponseDto> updateActiveUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDetailsDto detailsDto, @RequestParam Long actorId) {

        UserResponseDto updatedUser = userService.updateUserDetails(id,detailsDto, actorId);
        return ResponseEntity.ok(updatedUser);

    }

    @PatchMapping("/{id}/password") // @PatchMapping declares it is a partial update
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody UserUpdatePasswordDto passwordDto, @RequestParam Long actorId) {
        userService.updatePassword(id, passwordDto, actorId);
        return ResponseEntity.noContent().build();
    }


    /// DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @RequestParam Long actorId) {
        userService.deleteUser(id, actorId);
        return ResponseEntity.noContent().build();
    }
}