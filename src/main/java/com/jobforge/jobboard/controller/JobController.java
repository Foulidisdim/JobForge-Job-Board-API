package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.JobCreationDto;
import com.jobforge.jobboard.dto.JobResponseDto;
import com.jobforge.jobboard.dto.JobUpdateDto;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.service.ApplicationService;
import com.jobforge.jobboard.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    /// CREATE
    @PostMapping("/create")
    public ResponseEntity<JobResponseDto> createJob(@RequestParam Long userId, @Valid @RequestBody JobCreationDto jobDto) {

        JobResponseDto job = jobService.createJob(userId, jobDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @PostMapping("/{id}/duplicate") //CLOSED jobs only
    public ResponseEntity<JobResponseDto> duplicateClosedJob(
            @PathVariable Long id,
            @RequestParam Long actorId,
            @RequestParam JobStatus status // You can convert this to JobStatus in the service
    ) {
        JobResponseDto duplicatedJob = jobService.duplicateClosedJob(id, actorId, status);
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicatedJob);
    }

    @PostMapping("/{id}/repost") //ACTIVE jobs only.
    public ResponseEntity<JobResponseDto> repostJob(@PathVariable Long id, @RequestParam Long actorId) {
        JobResponseDto repostedJob = jobService.repostJob(id, actorId);
        return ResponseEntity.ok(repostedJob);
    }


    /// READ
    @GetMapping("/all")
    public ResponseEntity<List<JobResponseDto>> getAllActiveJobs() {

        List<JobResponseDto> responseDtos = jobService.findAllActiveJobs();
        return ResponseEntity.status(HttpStatus.OK).body(responseDtos);
    }

    @GetMapping("/companyJobs")
    public ResponseEntity<List<JobResponseDto>> getCompanyJobsByStatus(@RequestParam Long userId, @RequestParam JobStatus status) {

        List<JobResponseDto> responseDtos = jobService.findCompanyJobsByStatus(userId, status);
        return ResponseEntity.status(HttpStatus.OK).body(responseDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobById(@PathVariable Long id) {

        JobResponseDto responseDto = jobService.getNonDeletedJobResponseById(id);
        return ResponseEntity.ok(responseDto);
    }

    // Requests for a specific job's applications.
    @GetMapping("/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponseDto>> getApplicationsByJob(@PathVariable Long jobId, @RequestParam Long actorId) {

        List<ApplicationResponseDto> applications = applicationService.getApplicationsByJob(jobId, actorId);
        return ResponseEntity.ok(applications);
    }


    /// UPDATE
    @PutMapping("/{id}/update")
    public ResponseEntity<JobResponseDto> updateJob(@PathVariable("id") Long jobId, @Valid @RequestBody JobUpdateDto jobUpdateDto, @RequestParam Long actorId) {

        JobResponseDto updatedJob = jobService.updateJob(jobId,jobUpdateDto,actorId);
        return ResponseEntity.ok(updatedJob);
    }


    /// DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id, @RequestParam Long actorId) {

        jobService.deleteJob(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
