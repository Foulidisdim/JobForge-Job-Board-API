package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.JobCreationDto;
import com.jobforge.jobboard.dto.JobResponseDto;
import com.jobforge.jobboard.dto.JobUpdateDto;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.service.ApplicationService;
import com.jobforge.jobboard.service.JobService;
import com.jobforge.jobboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final UserService userService;

    /// CREATE
    @PostMapping()
    public ResponseEntity<JobResponseDto> createJob(@Valid @RequestBody JobCreationDto jobDto, @AuthenticationPrincipal CustomUserDetails principal) {

        User creator = userService.findActiveUserById(principal.getId());
        JobResponseDto job = jobService.createJob(jobDto, creator, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @PostMapping("/{id}/duplicateAs") //CLOSED jobs only
    public ResponseEntity<JobResponseDto> duplicateClosedJob(
            @PathVariable Long id,
            @RequestParam JobStatus status,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {

        User actor = userService.findActiveUserById(principal.getId());
        JobResponseDto duplicatedJob = jobService.duplicateClosedJob(id, status, actor, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicatedJob);
    }

    @PostMapping("/{id}/repost") //ACTIVE jobs only.
    public ResponseEntity<JobResponseDto> repostJob(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        JobResponseDto repostedJob = jobService.repostJob(id, principal);
        return ResponseEntity.ok(repostedJob);
    }


    /// READ
    @GetMapping()
    public ResponseEntity<List<JobResponseDto>> getAllActiveJobs() {

        List<JobResponseDto> responseDtos = jobService.findAllActiveJobs();
        return ResponseEntity.status(HttpStatus.OK).body(responseDtos);
    }

    @GetMapping("/companyJobs")
    public ResponseEntity<List<JobResponseDto>> getCompanyJobsByStatus(@RequestParam JobStatus status, @AuthenticationPrincipal CustomUserDetails principal) {

        List<JobResponseDto> responseDtos = jobService.findCompanyJobsByStatus(status, principal);
        return ResponseEntity.status(HttpStatus.OK).body(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobById(@PathVariable Long id) {

        JobResponseDto responseDto = jobService.getNonDeletedJobResponseById(id);
        return ResponseEntity.ok(responseDto);
    }

    // Requests for a specific job's applications.
    @GetMapping("/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponseDto>> getApplicationsByJob(@PathVariable Long jobId, @AuthenticationPrincipal CustomUserDetails principal) {

        List<ApplicationResponseDto> applications = applicationService.getApplicationsByJob(jobId, principal);
        return ResponseEntity.ok(applications);
    }


    /// UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDto> updateJob(@PathVariable("id") Long jobId, @Valid @RequestBody JobUpdateDto jobUpdateDto, @AuthenticationPrincipal CustomUserDetails principal) {

        JobResponseDto updatedJob = jobService.updateJob(jobId,jobUpdateDto, principal);
        return ResponseEntity.ok(updatedJob);
    }


    /// DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {

        jobService.deleteJob(id, principal);
        return ResponseEntity.noContent().build();
    }
}