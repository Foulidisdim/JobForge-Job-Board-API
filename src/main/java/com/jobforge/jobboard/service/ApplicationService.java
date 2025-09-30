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
import com.jobforge.jobboard.mapstructmapper.ApplicationMapper;
import com.jobforge.jobboard.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final ApplicationMapper applicationMapper;

    private final JobService jobService;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    // TODO: Implement a file upload system for the candidate's resume (as well asa the profile and company pics).

    /// POST
    @Transactional
    public ApplicationResponseDto apply(ApplicationCreationDto applicationDto, Long userId) {
        User candidate = userService.findActiveUserById(userId);

        authorizationService.ensureRole(candidate, Role.CANDIDATE);

        // Concurrency of events: Check if the job is still active (employer/recruiter could have CLOSED it the moment a candidate sent an Apply request, for example)
        Job job = jobService.findNonDeletedJobById(applicationDto.getJobId());
        if (job.getStatus() != com.jobforge.jobboard.enums.JobStatus.ACTIVE) {
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
    public List<ApplicationResponseDto> getApplicationsByCandidate(Long actorId) {

        // Use service-to-service communication to find the user and authenticate.
        User candidate = userService.findActiveUserById(actorId);
        authorizationService.ensureRole(candidate, Role.CANDIDATE);

        List<Application> applications = applicationRepository.findByCandidateId(actorId);
        return applications.stream()
                .map( application -> {
                    ApplicationResponseDto applicationResponseDto = applicationMapper.toDto(application);
                    applicationResponseDto.setCandidate(null); // Explicitly skips the candidate here for efficiency. No need to fetch them each time when they view their own applications
                    return applicationResponseDto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponseDto> getApplicationsByJob(Long jobId, Long actorId) {

        Job job = jobService.findNonDeletedJobById(jobId);
        User actingUser = userService.findActiveUserById(actorId);

        // Authorization Check
        // Only an employer or a recruiter can view applications for a job.
        // Ensure the actor belongs to the same company as the job's associated one and has the EMPLOYER/RECRUITER role.
        authorizationService.ensureCompanyRole(actingUser,job.getCompany(), Role.EMPLOYER, Role.RECRUITER);

        List<Application> applications = applicationRepository.findByJob(job);

        // Convert to DTOs and return.
        return applications.stream()
                .map(applicationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponseDto findById(Long id, Long actorId) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        //Authentication
        User actor = userService.findActiveUserById(actorId);
        authorizationService.ensureApplicationAccess(actor, application);

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
    public ApplicationResponseDto updateApplication(Long applicationId, ApplicationUpdateDto updateDto, Long actorId) {

        Application application = findEntityById(applicationId);
        User actor = userService.findActiveUserById(actorId);

        // Only the Employer/Recruiter associated with the job posting's company can update status and notes of the applications!
        authorizationService.ensureCompanyRole(actor, application.getJob().getCompany(), Role.EMPLOYER, Role.RECRUITER);

        if (application.getJob().getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update applications for non-active jobs.");
        }

        applicationMapper.updateApplicationFromDto(updateDto, application);
        return applicationMapper.toDto(application); // JPA dirty checking saved the updated application
    }

    @Transactional
    public ApplicationResponseDto markUnderReview(Long applicationId, Long actorId) {
        Application application = findEntityById(applicationId);
        User actor = userService.findActiveUserById(actorId);


        authorizationService.ensureCompanyRole(actor, application.getJob().getCompany(), Role.EMPLOYER, Role.RECRUITER);

        if (application.getStatus() != ApplicationStatus.APPLIED) {
            throw new IllegalStateException("Only applications of status " + ApplicationStatus.APPLIED + " can be marked as " + ApplicationStatus.UNDER_REVIEW + ".");
        }

        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        return applicationMapper.toDto(application);
    }

    // Not exactly a SOFT delete, as withdrawn applications will still be visible to
    // both candidates and recruiters. More of a status change (Update).
    @Transactional
    public ApplicationResponseDto withdrawApplication(Long applicationId, Long actorId) {
        Application application = findEntityById(applicationId);

        // Only the candidate who submitted it can withdraw
        authorizationService.ensureCandidateSelf(actorId, application.getCandidate().getId());

        // Only pending applications (not accepted/rejected) can be withdrawn
        switch(application.getStatus()) {
            case APPLIED, UNDER_REVIEW -> application.setStatus(ApplicationStatus.WITHDRAWN);
            case REJECTED, ACCEPTED -> throw new IllegalStateException("Cannot withdraw a finalized application.");
            case WITHDRAWN -> throw new IllegalStateException("Application already withdrawn.");
        }


        return applicationMapper.toDto(application);
    }


    ///HARD DELETE (Admin only)
    @Transactional
    public void deleteApplication(Long applicationId, Long actorId) {
        User actor = userService.findActiveUserById(actorId);
        authorizationService.ensureAdmin(actor);

        //No soft deletion concept for applications. Use findById normally. Deletion is just for moderation and cleanup purposes
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Applications have ID's that tie them to jobs and users. Deletion won't affect the job/user object.
        // It's safe to perform a physical delete!
        applicationRepository.delete(application);
    }
}
