package com.jobforge.jobboard.service;

import com.jobforge.jobboard.dto.ApplicationCreationDto;
import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.ApplicationUpdateDto;
import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.ApplicationStatus;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import com.jobforge.jobboard.exception.UnauthorizedException;
import com.jobforge.jobboard.mapstructmapper.ApplicationMapper;
import com.jobforge.jobboard.repository.ApplicationRepository;
import com.jobforge.jobboard.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final ApplicationMapper applicationMapper;

    private final JobService jobService;
    private final UserService userService;

    // TODO: Implement a file upload system for the candidate's resume (as well asa the profile and company pics).

    /// POST
    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponseDto apply(ApplicationCreationDto applicationDto, CustomUserDetails principal) {
        User candidate = userService.findActiveUserById(principal.getId());

        // Concurrency of events: Check if the job is still active (E.g., the employer/recruiter could have CLOSED it, moments before the candidate sent an Apply request).
        Job job = jobService.findNonDeletedJobById(applicationDto.getJobId());
        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Cannot apply to an inactive job.");
        }

        // Check if the candidate has already applied
        if(applicationRepository.existsByJobAndCandidate(job, candidate)) {
            throw new IllegalStateException("You have already applied to this job.");
        }

        // Maps the resume url from the creationDTO. JobId field doesn't exist in the entity and is ignored when mapping to entity.
        // BUT it is necessary to find the job that actually is present as a field (column) in the entity!
        Application application = applicationMapper.toEntity(applicationDto);

        application.setCandidate(candidate);
        application.setJob(job);

        Application savedApplication = applicationRepository.save(application);
        return applicationMapper.toDto(savedApplication);
    }


    ///GET
    //Candidate-only method
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<ApplicationResponseDto> getApplicationsByCandidate(CustomUserDetails candidate) {

        List<Application> applications = applicationRepository.findByCandidateId(candidate.getId());
        return applications.stream()
                .map( application -> {
                    ApplicationResponseDto applicationResponseDto = applicationMapper.toDto(application);
                    applicationResponseDto.setCandidate(null); // Explicitly skips the candidate here for efficiency. No need to fetch them each time when they view their own applications
                    return applicationResponseDto;
                })
                .collect(Collectors.toList());
    }

    // Only an employer or a recruiter from the job's company can view its applications.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('EMPLOYER', 'RECRUITER')")
    public List<ApplicationResponseDto> getApplicationsByJob(Long jobId, CustomUserDetails principal) {

        // Load job only once.
        Job job = jobService.findNonDeletedJobById(jobId);

        // Manual authorization check. Did not check inside @PreAuthorize because we would need to hit the db for the job there too.
        if (!Objects.equals(job.getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not allowed to view applications for this job.");
        }

        List<Application> applications = applicationRepository.findByJob(job);

        // Convert to DTOs and return.
        return applications.stream()
                .map(applicationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponseDto findById(Long id, CustomUserDetails principal) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        //Authentication (complex, can't be on @PreAuthorize)
        User user = userService.findActiveUserById(principal.getId());
        ensureApplicationAccess(user, application);

        return applicationMapper.toDto(application);
    }

    @Transactional(readOnly = true)
    public Application findEntityById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }


    /// UPDATE
    // Accept/Reject application or share feedback with the candidate (employers/recruiters method only)
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'RECRUITER')")
    public ApplicationResponseDto updateApplication(Long applicationId, ApplicationUpdateDto updateDto, CustomUserDetails principal) {

        Application application = findEntityById(applicationId);

        if (application.getJob().getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update applications for non-active jobs.");
        }

        // Manual authorization check. Did not check inside @PreAuthorize because we would need to hit the db for the application there too.
        if (!Objects.equals(application.getJob().getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not allowed to view applications for this job.");
        }

        applicationMapper.updateApplicationFromDto(updateDto, application);
        return applicationMapper.toDto(application); // JPA dirty checking saved the updated application
    }

    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'RECRUITER')")
    public ApplicationResponseDto markUnderReview(Long applicationId, CustomUserDetails principal) {
        Application application = findEntityById(applicationId);

        if (application.getStatus() != ApplicationStatus.APPLIED) {
            throw new IllegalStateException("Only applications of status " + ApplicationStatus.APPLIED + " can be marked as " + ApplicationStatus.UNDER_REVIEW + ".");
        }

        // Manual authorization check. Did not check inside @PreAuthorize because we would need to hit the db for the application there too.
        if (!Objects.equals(application.getJob().getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not allowed to manage applications for this job.");
        }

        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        return applicationMapper.toDto(application);
    }

    // Not exactly a SOFT delete, as withdrawn applications will still be visible to both candidates and recruiters. More of a status change (Update).
    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponseDto withdrawApplication(Long applicationId, CustomUserDetails principal) {

        // Load the application once
        Application application = findEntityById(applicationId);

        // Manual authorization: only the candidate who submitted it can withdraw
        if (!Objects.equals(application.getCandidate().getId(), principal.getId())) {
            throw new UnauthorizedException("You can only withdraw your own application.");
        }

        // Only pending applications(not accepted/rejected) can be withdrawn
        switch (application.getStatus()) {
            case APPLIED, UNDER_REVIEW -> application.setStatus(ApplicationStatus.WITHDRAWN);
            case REJECTED, ACCEPTED -> throw new IllegalStateException("Cannot withdraw a finalized application.");
            case WITHDRAWN -> throw new IllegalStateException("Application already withdrawn.");
        }

        return applicationMapper.toDto(application);
    }


    ///HARD DELETE (Admin only)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteApplication(Long applicationId) {

        //No soft deletion concept for applications. Use findById normally. Deletion is just for moderation and cleanup purposes
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Application entities have only ID's for the jobs and users they are tied to.
        // Normal deletion won't affect these job/user object and is safe.
        applicationRepository.delete(application);
    }



    /// -- Helper Methods --
    private void ensureApplicationAccess(User actor, Application application) {
        // Case 1: The actor is an admin.
        if (actor.getRole() == Role.ADMIN) {
            return;
        }

        // Case 2: The actor is the candidate who owns the application.
        if (Objects.equals(actor.getId(), application.getCandidate().getId())) {
            return;
        }

        // Case 3: The actor is a recruiter or employer from the same company that owns the job.
        if (actor.getRole() == Role.EMPLOYER || actor.getRole() == Role.RECRUITER) {
            if (Objects.equals(actor.getCompany(), application.getJob().getCompany())) {
                return;
            }
        }

        // If none of the above conditions are met, throw an exception.
        throw new UnauthorizedException("You do not have permission to view this application.");
    }
}