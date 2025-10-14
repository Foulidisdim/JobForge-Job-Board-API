package com.jobforge.jobboard.service;

import com.jobforge.jobboard.dto.JobCreationDto;
import com.jobforge.jobboard.dto.JobResponseDto;
import com.jobforge.jobboard.dto.JobUpdateDto;
import com.jobforge.jobboard.entity.Company;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.entity.Skill;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.EnumSubset;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.exception.RepostLimitExceededException;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import com.jobforge.jobboard.exception.UnauthorizedException;
import com.jobforge.jobboard.mapstructmapper.JobMapper;
import com.jobforge.jobboard.repository.JobRepository;
import com.jobforge.jobboard.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;

    private final JobMapper jobMapper;

    private final SkillService skillService;

    /// CREATE
    //Creates a new job posting from a DTO.
    // The `status` will be set by the client (DRAFT or ACTIVE only).
    @Transactional
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public JobResponseDto createJob(JobCreationDto creationDto, User creator, CustomUserDetails principal) {

        if (creationDto.getSalaryMin() > creationDto.getSalaryMax()) {
            throw new IllegalArgumentException("Maximum salary must be greater than or equal to minimum salary.");
        }

        Company company = principal.getCompany();
        Job job = jobMapper.toEntity(creationDto);

        //Trim possible title start and end space chars
        job.setTitle(job.getTitle().trim());

        // Manual mapping for created/managed By as it's not in the DTO
        job.setCreatedBy(creator);
        job.setManagedBy(creator);
        //Manual addition of the Company object to the job object based on the ID.
        job.setCompany(company);

        // Fetch and set skills from the database using the provided IDs.
        List<Skill> skills = creationDto.getSkillIds().stream()
                .distinct() //remove duplicates
                .map(skillService::findById) // Returns Skill. Throws exception if not found.
                .collect(Collectors.toList());
        job.setSkills(skills);

        // In the case of a new job creation, The job isn't saved in the database yet.
        // I need to persist it in to the DB and get it back with its auto generated ID.
        // JPA dirty checking with @Transactional only works on already persisted data (e.g., in the case of an update)
        Job savedJob = jobRepository.save(job);

        return jobMapper.toDto(savedJob);
    }

    @Value("${job.repost.cooldown-days}")
    private int repostCooldownDays; //Used to define the repost days constraint
    // Must only apply on active jobs.
    @Transactional
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public JobResponseDto repostJob(Long jobId, CustomUserDetails principal) {
        Job job = findNonDeletedJobById(jobId);

        // Ensure employer/recruiter belongs ot the job's company.
        if (!Objects.equals(job.getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not authorized to duplicate jobs for this company.");
        }

        // Only active jobs can be reposted
        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Only active jobs can be reposted.");
        }

        // Check time constraint to prevent spam: X days since last action (creation/repost), number derived from the config properties!
        Instant lastAction = (job.getRepostedAt() != null)
                ? job.getRepostedAt()
                : job.getCreatedAt();
        if (lastAction.isAfter(Instant.now().minus(repostCooldownDays, ChronoUnit.DAYS))) {
            throw new RepostLimitExceededException(
                    String.format("This job can only be reposted once every %d days.", repostCooldownDays)
            );
        }

        job.setRepostedAt(Instant.now());

        return jobMapper.toDto(job);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public JobResponseDto duplicateClosedJob(Long jobId, @EnumSubset(enumClass = JobStatus.class, anyOf = { "DRAFT", "ACTIVE" }) JobStatus status, User actor, CustomUserDetails principal) {

        Job originalJob = findNonDeletedJobById(jobId);

        // Ensure employer/recruiter belongs ot the job's company.
        if (!Objects.equals(originalJob.getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not authorized to duplicate jobs for this company.");
        }

        // Only allow duplication of "CLOSED" jobs (jobs withdrawn from the company itself)
        if (originalJob.getStatus() != JobStatus.CLOSED) {
            throw new IllegalStateException("Only closed jobs can be duplicated.");
        }

        Job duplicateJob = copyJobFields(actor,originalJob,status);

        Job savedDuplicate = jobRepository.save(duplicateJob);
        return jobMapper.toDto(savedDuplicate);
    }


    /// UPDATE
    // Updates an existing job posting.
    @Transactional
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public JobResponseDto updateJob(Long jobId, JobUpdateDto updateDto, CustomUserDetails principal) {
        if (updateDto.getSalaryMin() > updateDto.getSalaryMax()) {
            throw new IllegalArgumentException("Maximum salary must be greater than or equal to minimum salary.");
        }

        Job job = findNonDeletedJobById(jobId);

        // Ensure employer/recruiter belongs ot the job's company.
        if (!Objects.equals(job.getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not authorized to duplicate jobs for this company.");
        }

        //Trim possible title start and end space chars
        job.setTitle(updateDto.getTitle().trim());
        jobMapper.updateJobFromDto(updateDto, job);

        // Explicitly update the skills if new IDs are provided.
        List<Skill> skills = updateDto.getSkillIds().stream()
                .distinct() //remove duplicates
                .map(skillService::findById)
                .collect(Collectors.toList());
        job.setSkills(skills);


        // JPA dirty checking handles updating the job in-memory and mapping it with the new data.
        // After and only if the operations are successful, the dirty Job object is saved into the DB!
        return jobMapper.toDto(job);
    }


    // TODO: Make an endpoint that uses this as a feature for the employer! Refactor with preauthorize and principal or just use this method inside a new one, specifically designed for the endpoint.
    /**
     * Transfers management rights for a set of jobs from the old manager to a new active employer.
     * @param jobManagerId The ID of the user whose management rights are being revoked.
     * @param companyId The ID of the company to scope the job search.
     * @param newManager The User who will take over management.
     */
    @Transactional
    public void transferJobManagement(Long jobManagerId, Long companyId, User newManager) {

        // This can happen inside the method that handles the company deletion when the company employer is deleted.
        if (Objects.equals(jobManagerId, newManager.getId())) {
            return;
        }

        // Find all jobs currently managed by the user specified by jobManagerId.
        // NOTE: Job management can be reassigned, so searching by managedBy instead of createdBy is correct.
        List<Job> jobsToTransfer = findJobsByManagedByIdAndCompanyId(jobManagerId, companyId);

        if (jobsToTransfer.isEmpty()) {
            return;
        }
        // Implicit save thanks to @Transactional and JPA dirty checking.
        jobsToTransfer.forEach(job -> job.setManagedBy(newManager));
    }


    /// DELETE
    // We soft-delete the job posting by setting the status as "DELETED".
    // Keep the file for historical reasons.
    @Transactional
    public void deleteJob(Long jobId, CustomUserDetails principal) {
        Job job = findNonDeletedJobById(jobId);

        // Ensure employer/recruiter belongs ot the job's company.
        if (!Objects.equals(job.getCompany().getId(), principal.getCompany().getId())) {
            throw new UnauthorizedException("You are not authorized to duplicate jobs for this company.");
        }

        job.setStatus(JobStatus.DELETED); // JPA dirty checking will handle the save!
    }


    /// GET
    //Finds all active job postings for display to candidates.
    @Transactional(readOnly = true)
    public List<JobResponseDto> findAllActiveJobs() {
        List<Job> jobs = jobRepository.findAllByStatus(JobStatus.ACTIVE);
        return jobs.stream().map(jobMapper::toDto).collect(Collectors.toList());
    }

    // Company use only. VALIDATE so deleted jobs don't show up to the employers/candidates.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'RECRUITER')")
    public List<JobResponseDto> findCompanyJobsByStatus(@EnumSubset(enumClass = JobStatus.class, anyOf = {"ACTIVE", "DRAFT", "CLOSED"}) JobStatus status,  CustomUserDetails principal) {

        // Find ONLY the jobs associated with their current company
        List<Job> jobs = jobRepository.findAllByCompanyIdAndStatus(principal.getCompany().getId(), status);
        return jobs.stream().map(jobMapper::toDto).collect(Collectors.toList());
    }


    /// job-finding logic to avoid redundancy and service-to-service communication.
    @Transactional(readOnly = true)
    public Job findNonDeletedJobById(Long id) {
        return jobRepository.findByIdAndStatusNot(id, JobStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));
    }

    @Transactional(readOnly = true)
    public Job findJobByIdAndStatus(Long id, JobStatus status) {
        return jobRepository.findByIdAndStatus(id, status)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with status: " + status.name()));
    }

    @Transactional
    public List<Job> findJobsByManagedByIdAndCompanyId (Long jobManagerId, Long companyId) {
        return jobRepository.findAllByManagedByIdAndCompanyId(jobManagerId, companyId);
    }


    @Transactional(readOnly = true)
    public JobResponseDto getNonDeletedJobResponseById(Long id) {
        return jobMapper.toDto(findNonDeletedJobById(id));
    }



    /// -- Helper methods --
    private static Job copyJobFields(User actor, Job originalJob, JobStatus status) {
        Company company = actor.getCompany();

        Job duplicateJob = new Job();

        // Copy basic fields
        duplicateJob.setTitle(originalJob.getTitle());
        duplicateJob.setLocation(originalJob.getLocation());
        duplicateJob.setDescription(originalJob.getDescription());
        duplicateJob.setEmploymentType(originalJob.getEmploymentType());
        duplicateJob.setExperienceLevel(originalJob.getExperienceLevel());
        duplicateJob.setWorkArrangement(originalJob.getWorkArrangement());
        duplicateJob.setSalaryMin(originalJob.getSalaryMin());
        duplicateJob.setSalaryMax(originalJob.getSalaryMax());
        duplicateJob.setCurrencyCode(originalJob.getCurrencyCode());

        // Copy skills
        duplicateJob.setSkills(new ArrayList<>(originalJob.getSkills()));

        // Set creator and company
        duplicateJob.setCreatedBy(actor);
        duplicateJob.setManagedBy(actor);
        duplicateJob.setCompany(company);

        // DRAFT or ACTIVE according to the frontEnd's message
        duplicateJob.setStatus(status);
        return duplicateJob;
    }
}