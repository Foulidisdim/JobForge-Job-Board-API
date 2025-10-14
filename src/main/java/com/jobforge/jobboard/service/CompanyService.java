package com.jobforge.jobboard.service;

import com.jobforge.jobboard.dto.CompanyCreationDto;
import com.jobforge.jobboard.dto.CompanyResponseDto;
import com.jobforge.jobboard.dto.CompanyUpdateDto;
import com.jobforge.jobboard.dto.UserResponseDto;
import com.jobforge.jobboard.entity.Company;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import com.jobforge.jobboard.mapstructmapper.CompanyMapper;
import com.jobforge.jobboard.mapstructmapper.UserMapper;
import com.jobforge.jobboard.repository.CompanyRepository;
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
public class CompanyService {
    private final CompanyRepository companyRepository;

    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;

    private final JobService jobService;
    private final BusinessRuleService businessRuleService;

    /// POST
    @Transactional
    @PreAuthorize("!hasAnyAuthority('EMPLOYER', 'RECRUITER')") // Business Rule: A user cannot create a company if they are already associated with one (check with no db hit!).
    public CompanyResponseDto createCompany(CompanyCreationDto companyDto, User founder /* Derived by the signed-in user from spring security*/ ) {

        founder.setRole(Role.EMPLOYER); //becomes employer when they create their company! Automatically saved because of the transactional annotation and JPA dirty checking!
        Company company = companyMapper.toEntity(companyDto);
        company.setRelatedUsers(List.of(founder));

        company.setEmployer(founder);
        founder.setCompany(company);

        Company savedCompany = companyRepository.save(company);

        // Invalidate all token to enforce a re-login for role refresh.
        businessRuleService.invalidateAllAuthenticationTokens(founder);

        return companyMapper.toDto(savedCompany);
    }


    /// UPDATE
    // @PreAuthorize and the authenticated principal seamlessly forced changes only to an Employers OWN company!
    @Transactional
    @PreAuthorize("hasAuthority('EMPLOYER')")
    public UserResponseDto appointRecruiter(User userToAppoint, CustomUserDetails principal) {


        // Business Rule Validation
        // 1. A user with an existing company association (i.e., a recruiter or its employer) cannot be a recruiter.
        // 2. This also enforces the "one company per recruiter" business rule.
        if (userToAppoint.getCompany() != null) {
            throw new IllegalStateException("User is already an Employer or Recruiter. Please try appointing a different user.");
        }

        // Assign the RECRUITER role to the user.
        userToAppoint.setRole(Role.RECRUITER);
        // Smartly associate the user with the employer's company.
        // Safe because only an authenticated employer (and only for their company) can call this method!
        userToAppoint.setCompany(principal.getCompany());

        // Invalidate all token to enforce a re-login for role refresh.
        businessRuleService.invalidateAllAuthenticationTokens(userToAppoint);

        // Return a DTO to confirm the successful appointment.
        return userMapper.toDto(userToAppoint); // Assumes you have a UserMapper.
    }

    @Transactional
    @PreAuthorize("hasAuthority('EMPLOYER')")
    public CompanyResponseDto updateCompany(CompanyUpdateDto updateDto, CustomUserDetails principal) { // Only for their own company.
        Company company = principal.getCompany();

        companyMapper.updateCompanyFromDto(updateDto, company);
        return companyMapper.toDto(company);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EMPLOYER')") // Only for their own company.
    public UserResponseDto removeRecruiter(User recruiter, CustomUserDetails principal) {


        // Check that the user is actually a recruiter for this company.
        // This prevents a user from removing a recruiter from a company they aren't part of.
        if (!recruiter.getRole().equals(Role.RECRUITER) || recruiter.getCompany() == null || !Objects.equals(recruiter.getCompany().getId(), principal.getCompany().getId())) {
            throw new IllegalStateException("User is not a recruiter for this company and cannot be removed.");
        }

        // Transfer all jobs to the current employer instead of altering their status!
        jobService.transferJobManagement(recruiter.getId(),recruiter.getCompany().getId(), recruiter.getCompany().getEmployer());

        // Remove the company association and revert the user's role to CANDIDATE.
        recruiter.setCompany(null);
        recruiter.setRole(Role.CANDIDATE);

        // Revoke all long-term tokens and invalidate the session instantly
        businessRuleService.invalidateAllAuthenticationTokens(recruiter);

        // Return the updated user DTO
        return userMapper.toDto(recruiter);
    }

    @Transactional
    @PreAuthorize("hasAuthority('EMPLOYER')")
    public CompanyResponseDto changeCompanyEmployer(User currentEmployer, User newEmployer, CustomUserDetails principal) { // Only for their own company.

        Company company = principal.getCompany();

        if (newEmployer.getRole().equals(Role.EMPLOYER)) {
            throw new IllegalStateException("User is already an employer and cannot become an employer for another company.");
        }
        if (Objects.equals(newEmployer.getId(), currentEmployer.getId())) {
            throw new IllegalArgumentException("The new employer cannot be the same as the current employer.");
        }

        // Transfer any jobs under the old employer's management
        jobService.transferJobManagement(currentEmployer.getId(), currentEmployer.getCompany().getId(), newEmployer);

        // Update the roles and company associations
        // Demote the current employer to a candidate and appoint the new employer
        currentEmployer.setRole(Role.CANDIDATE);
        newEmployer.setRole(Role.EMPLOYER);

        // Set the company associations!
        currentEmployer.setCompany(null);
        newEmployer.setCompany(company);
        company.setEmployer(newEmployer);

        // Revoke all long-term tokens and invalidate the session instantly
        businessRuleService.invalidateAllAuthenticationTokens(currentEmployer);

        // Return the updated company DTO. JPA dirty checking will handle the persistence of both user entities.
        return companyMapper.toDto(company);
    }


    /// DELETE
    @Transactional
    @PreAuthorize("hasAuthority('EMPLOYER')")
    public void deleteCompany(CustomUserDetails principal) {
        // An employer can only be associated WITH ONE company they create. They can appoint MANY recruiters with their company,
        // but each recruiter CANNOT work as a recruiter to other companies. When an employer deletes a company, I must:
        // - Disassociate All the related users (employers and recruiters)
        // - Mark the jobs they made for the company AS DELETED (soft-delete).
        // - DON'T delete any users, just make them a "CANDIDATE" so they can still use the normal job finding services.

        Company company = principal.getCompany();

        for(User user : company.getRelatedUsers()) {
            user.setCompany(null);
            user.setRole(Role.CANDIDATE);
            businessRuleService.invalidateAllAuthenticationTokens(user); // CRITICAL to revoke access here!
        }

        // Soft-delete all jobs associated with the company.
        markAllCompanyJobsAsDeleted(company);

        //Soft delete the company itself (saving is automatically done by JPAs dirty checking)
        company.setDeleted(true);
    }

    /// GET
    @Transactional(readOnly = true)
    public CompanyResponseDto getActiveCompanyResponseDtoById(Long companyId) {
        Company company = findActiveCompanyById(companyId);
        return companyMapper.toDto(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponseDto> getAllActiveCompanyResponseDtos() {
        return companyRepository.findAllByDeletedFalse().stream()
                .map(companyMapper::toDto)  // convert each Company → CompanyResponseDto
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EMPLOYER')")
    public List<UserResponseDto> getActiveRecruitersForCompany(CustomUserDetails principal) {

        // Can only view the recruiters of their own company.
        Company company = principal.getCompany();

        // Fetch the associated recruiters.
        return company.getRelatedUsers().stream()
                .filter(user -> user.getRole().equals(Role.RECRUITER))
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }


    /// Internal service-to-service data exchange methods (separation of concerns)
    @Transactional(readOnly = true)
    public Company findActiveCompanyById(Long companyId) {
        return companyRepository.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }


    /// -- Helper Methods --
    @Transactional
    public void handleCompanySoftDeletionBecauseOfEmployerDeletion(Company company) {

        for(User user : company.getRelatedUsers()) {
            user.setCompany(null);
            user.setRole(Role.CANDIDATE);
            businessRuleService.invalidateAllAuthenticationTokens(user); // CRITICAL to revoke access here!
        }

        // Soft-delete all jobs associated with the company.
        markAllCompanyJobsAsDeleted(company);

        //Soft delete the company itself (saving is automatically done by JPAs dirty checking)
        company.setDeleted(true);
    }

    @Transactional
    public void markAllCompanyJobsAsDeleted(Company company) {
        // Assuming JobStatus has a value like ACTIVE
        List<Job> activeJobs = company.getJobs();

        if (activeJobs.isEmpty()) {
            return;
        }

        activeJobs.forEach(job -> job.setStatus(JobStatus.DELETED));
    }
}