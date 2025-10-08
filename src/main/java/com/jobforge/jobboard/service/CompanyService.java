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

    private final UserService userService;

    /// POST
    @Transactional
    @PreAuthorize("!hasRole('EMPLOYER')") // Business Rule: A user cannot create a company if they are already an employer (check with no db hit!).
    public CompanyResponseDto createCompany(CompanyCreationDto companyDto, CustomUserDetails principal/* Derived by the signed-in user from spring security*/ ) {

        // If business rule is valid, THEN do the required db search to update the user entity.
        User user = userService.findActiveUserById(principal.getId());

        user.setRole(Role.EMPLOYER); //becomes employer when they create their company! Automatically saved because of the transactional annotation and JPA dirty checking!
        Company company = companyMapper.toEntity(companyDto);
        company.setRelatedUsers(List.of(user));

        /// createdByUserId comes from the authenticated user (principal), not the client.
        /// It is manually set here because MapStruct cannot access the authenticated user context.
        /// Setting it here is safe because the user has already been validated and business rules applied.
        company.setCreatedByUserId(principal.getId());

        user.setCompany(company);

        Company savedCompany = companyRepository.save(company);

        return companyMapper.toDto(savedCompany);
    }


    /// UPDATE
    // @PreAuthorize and the authenticated principal seamlessly forced changes only to an Employers OWN company!
    @Transactional
    @PreAuthorize("hasRole('EMPLOYER')")
    public UserResponseDto appointRecruiter(Long newRecruiterId, CustomUserDetails principal) {

        User userToAppoint = userService.findActiveUserById(newRecruiterId);

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

        // Return a DTO to confirm the successful appointment.
        return userMapper.toDto(userToAppoint); // Assumes you have a UserMapper.
    }

    @Transactional
    @PreAuthorize("hasRole('EMPLOYER')")
    public CompanyResponseDto updateCompany(CompanyUpdateDto updateDto, CustomUserDetails principal) { // Only for their own company.
        Company company = principal.getCompany();

        companyMapper.updateCompanyFromDto(updateDto, company);
        return companyMapper.toDto(company);
    }

    @Transactional
    @PreAuthorize("hasRole('EMPLOYER')") // Only for their own company.
    public UserResponseDto removeRecruiter(Long recruiterId, CustomUserDetails principal) {

        User recruiter = userService.findActiveUserById(recruiterId);

        // Check that the user is actually a recruiter for this company.
        // This prevents a user from removing a recruiter from a company they aren't part of.
        if (!recruiter.getRole().equals(Role.RECRUITER) || recruiter.getCompany() == null || !Objects.equals(recruiter.getCompany().getId(), principal.getCompany().getId())) {
            throw new IllegalStateException("User is not a recruiter for this company and cannot be removed.");
        }

        // Remove the company association and revert the user's role to CANDIDATE.
        recruiter.setCompany(null);
        recruiter.setRole(Role.CANDIDATE);

        // Return the updated user DTO
        return userMapper.toDto(recruiter);
    }

    @Transactional
    @PreAuthorize("hasRole('EMPLOYER')")
    public CompanyResponseDto changeCompanyEmployer(Long newEmployerId, CustomUserDetails principal) { // Only for their own company.
        // Only the current employer of the company can appoint a new one.
        User currentEmployer = userService.findActiveUserById(principal.getId());
        Company company = principal.getCompany();
        User newEmployer = userService.findActiveUserById(newEmployerId);


        if (newEmployer.getRole().equals(Role.EMPLOYER)) {
            throw new IllegalStateException("User is already an employer and cannot become an employer for another company.");
        }
        if (Objects.equals(newEmployer.getId(), currentEmployer.getId())) {
            throw new IllegalArgumentException("The new employer cannot be the same as the current employer.");
        }

        // Update the roles and company associations
        // Demote the current employer to a candidate and appoint the new employer
        currentEmployer.setRole(Role.CANDIDATE);
        newEmployer.setRole(Role.EMPLOYER);

        // Set the company associations!
        currentEmployer.setCompany(null);
        newEmployer.setCompany(company);

        // Return the updated company DTO. JPA dirty checking will handle the persistence of both user entities.
        return companyMapper.toDto(company);
    }


    /// DELETE
    @Transactional
    public void deleteCompany(CustomUserDetails principal) {
        // An employer can only be associated WITH ONE company they create. They can appoint MANY recruiters with their company,
        // but each recruiter CANNOT work as a recruiter to other companies. When I delete a company, I must:
        // A. disassociate All the related users (employers and recruiters)
        // B. Mark the jobs they made for the company AS DELETED (soft-delete).
        // I DON'T delete any users, I just make them a "CANDIDATE" so they can still use the normal job finding services.

        Company company = principal.getCompany();

        for(User user : company.getRelatedUsers()) {
            user.setCompany(null); // Remove company association
            user.setRole(Role.CANDIDATE); // Change Role to CANDIDATE
        }

        // Soft-delete all jobs associated with the company.
        for (Job job : company.getJobs()) {
            job.setStatus(JobStatus.DELETED);
        }

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
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<UserResponseDto> getActiveRecruitersForCompany(CustomUserDetails principal) {

        // Can only view the recruiters of their own company.
        Company company = principal.getCompany();

        // Fetch the associated recruiters.
        return company.getRelatedUsers().stream()
                .filter(user -> user.getRole().equals(Role.RECRUITER))
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyResponseDto getCompanyForUser(Long actorId) {

        User user = userService.findActiveUserById(actorId);

        // The user must be associated with a company.
        if (user.getCompany() == null) {
            throw new IllegalStateException("User is not associated with any company.");
        }

        return companyMapper.toDto(user.getCompany());
    }


    /// Internal service-to-service data exchange methods (separation of concerns)
    @Transactional(readOnly = true)
    public Company findActiveCompanyById(Long companyId) {
        return companyRepository.findByIdAndDeletedFalse(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
    }
}
