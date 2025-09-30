package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.ApplicationCreationDto;
import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.ApplicationUpdateDto;
import com.jobforge.jobboard.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /// CREATE
    @PostMapping("/apply")
    public ResponseEntity<ApplicationResponseDto> createApplication(@Valid @RequestBody ApplicationCreationDto creationDto, @RequestParam Long actorId) {

        ApplicationResponseDto applicationResponse = applicationService.apply(creationDto, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationResponse);
    }


    /// READ
    @GetMapping("/myApplications")
    public ResponseEntity<List<ApplicationResponseDto>> getAllCandidateApplications(@RequestParam Long id) {
        return ResponseEntity.ok(applicationService.getApplicationsByCandidate(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getApplicationById(@PathVariable Long id, @RequestParam Long actorId) {
        return ResponseEntity.ok(applicationService.findById(id, actorId));
    }


    ///UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> updateApplication(@PathVariable Long id,
                                                                    @Valid @RequestBody ApplicationUpdateDto updateDto, @RequestParam Long actorId) {
        ApplicationResponseDto updatedApplication = applicationService.updateApplication(id, updateDto, actorId);

        return ResponseEntity.ok(updatedApplication);
    }

    // Accessible by the employer/recruiter of the associated company only.
    @PatchMapping("/applications/{id}/markUnderReview")
    public ResponseEntity<ApplicationResponseDto> markUnderReview(@PathVariable Long id, @RequestParam Long actorId
    ) {
        ApplicationResponseDto dto = applicationService.markUnderReview(id, actorId);
        return ResponseEntity.ok(dto);
    }

    // Accessible by the candidate only.
    @PatchMapping ("/{id}/withdraw")
    public ResponseEntity<ApplicationResponseDto> withdrawApplication(@PathVariable Long id,
                                                                      @RequestParam Long actorId) {
        ApplicationResponseDto withdrawn = applicationService.withdrawApplication(id, actorId);
        return ResponseEntity.ok(withdrawn);
    }


    /// DELETE (If admin found it as non-conforming to the JobForge terms of use)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id, @RequestParam Long actorId) {

        applicationService.deleteApplication(id, actorId);
        return ResponseEntity.noContent().build();

    }

}

