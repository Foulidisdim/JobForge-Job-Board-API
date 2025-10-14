package com.jobforge.jobboard.service;

import com.jobforge.jobboard.dto.ApplicationCreationDto;
import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.ApplicationUpdateDto;
import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.ApplicationStatus;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import com.jobforge.jobboard.mapstructmapper.ApplicationMapper;
import com.jobforge.jobboard.repository.ApplicationRepository;
import com.jobforge.jobboard.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final BusinessRuleService businessRuleService;

    // TODO: Implement a file upload system for the candidate's resume (as well asa the profile and company pics).

    /// POST
    // TODO: Expand the application's rights to recruiters/employers also. they should be able to search and apply for jobs too. Just edit/make new workflows for their data after they leave their current company.
    @Transactional
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public ApplicationResponseDto apply(ApplicationCreationDto applicationDto, User candidate) {

        Long jobId = applicationDto.getJobId();

        // Concurrency of events: Check if the job is still active (E.g., the employer/recruiter could have CLOSED it, moments before the candidate sent an Apply request).
        Job job = jobService.findJobByIdAndStatus(jobId, JobStatus.ACTIVE);

        // Check if the candidate has already applied (only check active users and jobs)
        if(applicationRepository.existsByJob_IdAndJob_StatusAndCandidate_IdAndCandidate_DeletedFalse(jobId,JobStatus.ACTIVE,candidate.getId())){
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
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public List<ApplicationResponseDto> getMyApplications(CustomUserDetails candidate) {

        List<Application> applications = applicationRepository.findAllByCandidateIdAndCandidateDeletedFalse(candidate.getId());
        return applications.stream()
                .map( application -> {
                    ApplicationResponseDto applicationResponseDto = applicationMapper.toDto(application);
                    applicationResponseDto.setCandidate(null); // Explicitly skips the candidate here for efficiency. No need to fetch them each time when they view their own applications.
                    return applicationResponseDto;
                })
                .collect(Collectors.toList());
    }

    // Only an employer or a recruiter from the job's company can view its applications.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public List<ApplicationResponseDto> getApplicationsByJob(Long jobId, CustomUserDetails principal) {

        Job job = jobService.findNonDeletedJobById(jobId);

        // Business-rule-driven authorization check. Did not check inside @PreAuthorize because we would need to hit the db for the job there too.
        businessRuleService.canManageJobApplications(job.getCompany().getId(), principal);

        List<Application> applications = applicationRepository.findAllByJobId(jobId);

        // Convert to DTOs and return.
        return applications.stream()
                .map(applicationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponseDto getById(Long id, CustomUserDetails principal) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        // Authorization check (Admin, candidate of application, Company personnel)
        businessRuleService.ensureApplicationAccess(application, principal);

        return applicationMapper.toDto(application);
    }


    /// UPDATE
    // Share feedback with the candidate and/or Accept/Reject application (employers/recruiters access only)
    @Transactional
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public ApplicationResponseDto updateApplication(Long applicationId, ApplicationUpdateDto updateDto, CustomUserDetails principal) {

        Application application = applicationRepository.findByIdAndStatusIn(applicationId, ApplicationStatus.APPLIED, ApplicationStatus.UNDER_REVIEW)
                .orElseThrow(() -> new ResourceNotFoundException( "Application not found with ID: " + applicationId +
                        " or is not in a status that permits modification"));

        // Authorization check.
        businessRuleService.canManageJobApplications(application.getJob().getCompany().getId(), principal);

        if (application.getJob().getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update applications for non-active jobs.");
        }

        // If the update DTO doesn't include a status change to ACCEPTED/REJECTED was made and only feedback was shared,
        // mark the application as "UNDER_REVIEW" to at least inform the candidate.
        if(updateDto.getStatus() == null){
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
        }

        applicationMapper.updateApplicationFromDto(updateDto, application);
        return applicationMapper.toDto(application); // JPA dirty checking saved the updated application
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public ApplicationResponseDto markUnderReview(Long applicationId, CustomUserDetails principal) {

        Application application = applicationRepository.findByIdAndStatusIn(applicationId, ApplicationStatus.APPLIED)
                .orElseThrow(() -> new ResourceNotFoundException( "Application not found or is not of status:" + ApplicationStatus.APPLIED.name()));

        // Authorization check.
        businessRuleService.canManageJobApplications(application.getJob().getCompany().getId(), principal);

        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        return applicationMapper.toDto(application);
    }

    // "SOFT" delete, but withdrawn applications will still be visible to both candidates and recruiters. More of a status change (Update).
    @Transactional
    @PreAuthorize("hasAuthority('CANDIDATE')")
    public ApplicationResponseDto withdrawApplication(Long applicationId, CustomUserDetails principal) {

        // PreAuthorize CANDIDATE + Authenticated principal + Query ensure That an application will be found
        // ONLY if the candidate ID matches the principal's ID AND the candidate is not deleted!
        // NO NEED FOR FURTHER AUTHORIZATION CHECKS!
        Application application = applicationRepository.findByIdAndCandidateIdAndCandidate_DeletedFalse(applicationId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found, or you are not authorized to manage this application."));

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
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteApplication(Long applicationId) {

        //No soft deletion concept for applications. Use findById normally. Deletion is just for moderation and cleanup purposes
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Application entities have only ID's for the jobs and users they are tied to.
        // Normal deletion won't affect these job/user object and is safe.
        applicationRepository.delete(application);
    }


    /// -- Helper Methods --
    @Transactional
    public void handleUserAccountDeletionApplicationsCleanup(Long deletedUserId) {
        // Find all non finalized applications for the user and WITHDRAW them.

        // Define statuses that should NOT be touched ("finalized" states)
        ApplicationStatus[] excludedStatuses = {
                ApplicationStatus.REJECTED,
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.WITHDRAWN
        };

        List<Application> applicationsToWithdraw = applicationRepository.findAllByCandidateIdAndStatusNotIn(deletedUserId, excludedStatuses);

        // SET TO WITHDRAWN
        if(!applicationsToWithdraw.isEmpty()){
            for (Application app : applicationsToWithdraw) {
                app.setStatus(ApplicationStatus.WITHDRAWN);
            }
        }
    }
}