package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.ApplicationCreationDto;
import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.ApplicationUpdateDto;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.service.ApplicationService;
import com.jobforge.jobboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    /// CREATE
    @PostMapping()
    public ResponseEntity<ApplicationResponseDto> createApplication(@Valid @RequestBody ApplicationCreationDto creationDto, @AuthenticationPrincipal CustomUserDetails principal) {

        User candidate = userService.findActiveUserById(principal.getId());
        ApplicationResponseDto applicationResponse = applicationService.apply(creationDto, candidate);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationResponse);
    }


    /// READ
    @GetMapping("/myApplications")
    public ResponseEntity<List<ApplicationResponseDto>> getAllCandidateApplications(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(applicationService.getMyApplications(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(applicationService.getById(id, principal));
    }


    ///UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> updateApplication(@PathVariable Long id,
                                                                    @Valid @RequestBody ApplicationUpdateDto updateDto, @AuthenticationPrincipal CustomUserDetails principal) {
        ApplicationResponseDto updatedApplication = applicationService.updateApplication(id, updateDto, principal);

        return ResponseEntity.ok(updatedApplication);
    }

    // Accessible by the employer/recruiter of the associated company only.
    @PatchMapping("/{id}/markUnderReview")
    public ResponseEntity<ApplicationResponseDto> markUnderReview(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        ApplicationResponseDto dto = applicationService.markUnderReview(id, principal);
        return ResponseEntity.ok(dto);
    }

    // Accessible by the candidate only.
    @PatchMapping ("/{id}/withdraw")
    public ResponseEntity<ApplicationResponseDto> withdrawApplication(@PathVariable Long id,
                                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        ApplicationResponseDto withdrawn = applicationService.withdrawApplication(id, principal);
        return ResponseEntity.ok(withdrawn);
    }


    /// DELETE (If admin found it as non-conforming to the JobForge terms of use)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {

        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();

    }

}