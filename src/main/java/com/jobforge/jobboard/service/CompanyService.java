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
import lombok.RequiredArgsConstructor;
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
    private final AuthorizationService authorizationService;

    /// POST
    @Transactional
    public CompanyResponseDto createCompany(CompanyCreationDto companyDto, Long actorId /* Derived by the signed-in user from the JWT token*/ ) {
        User user = userService.findActiveUserById(actorId);

        // Business Rule: A user cannot create a company if they are already an employer.
        if (user.getRole().equals(Role.EMPLOYER)) {
            throw new IllegalStateException("User is already an employer and cannot create a new company.");
        }

        user.setRole(Role.EMPLOYER); //becomes employer when they create their company! Automatically saved because of the transactional annotation and JPA dirty checking!
        Company company = companyMapper.toEntity(companyDto);
        company.setRelatedUsers(List.of(user));

        /// createdByUserId comes from the authenticated user (actorId), not the client.
        /// It is manually set here because MapStruct cannot access the authenticated user context.
        /// This prevents a malicious client from sending a fake ID and bypassing security rules.
        /// Setting it here is safe because the user has already been validated and business rules applied.
        // Use Objects.equals() to safely check if actorId is not null before setting it.
        if (Objects.equals(actorId, user.getId())) {
            company.setCreatedByUserId(actorId);
        }

        user.setCompany(company);

        Company savedCompany = companyRepository.save(company);

        return companyMapper.toDto(savedCompany);
    }


    /// UPDATE
    @Transactional
    public UserResponseDto appointRecruiter(Long companyId, Long recruiterId, Long actorId) {
        // Authorization check: Only the employer of this company can appoint a recruiter
        User actingUser = userService.findActiveUserById(actorId);
        Company company = findActiveCompanyById(companyId);
        authorizationService.ensureCompanyRole(actingUser, company, Role.EMPLOYER);

        User userToAppoint = userService.findActiveUserById(recruiterId);

        // Business Rule Validation
        // Check if the user is already associated with a company.
        // A user with an existing company association (i.e., a recruiter) or the EMPLOYER role cannot be a recruiter.
        // This enforces the "one company per recruiter" rule.
        if (userToAppoint.getCompany() != null || userToAppoint.getRole().equals(Role.EMPLOYER)) {
            throw new IllegalStateException("User is already an Employer or Recruiter. Please try appointing a different user.");
        }

        // Set the user as a recruiter and update their company association.
        userToAppoint.setRole(Role.RECRUITER);
        userToAppoint.setCompany(company);

        // Return a DTO to confirm the successful appointment.
        return userMapper.toDto(userToAppoint); // Assumes you have a UserMapper.
    }

    @Transactional
    public CompanyResponseDto updateCompany(CompanyUpdateDto updateDto, Long companyId, Long actorId) {
        // Employer authorization
        User actingUser = userService.findActiveUserById(actorId);
        Company company = findActiveCompanyById(companyId);
        authorizationService.ensureCompanyRole(actingUser, company, Role.EMPLOYER);

        companyMapper.updateCompanyFromDto(updateDto, company);
        return companyMapper.toDto(company);
    }

    @Transactional
    public UserResponseDto removeRecruiter(Long companyId, Long recruiterId, Long actorId) {
        // Authorization
        User actingUser = userService.findActiveUserById(actorId);
        Company company = findActiveCompanyById(companyId);
        authorizationService.ensureCompanyRole(actingUser, company, Role.EMPLOYER);

        User recruiter = userService.findActiveUserById(recruiterId);

        // Check that the user is actually a recruiter for this company.
        // This prevents a user from removing a recruiter from a company they aren't part of.
        if (!recruiter.getRole().equals(Role.RECRUITER) || recruiter.getCompany() == null || !recruiter.getCompany().equals(company)) {
            throw new IllegalStateException("User is not a recruiter for this company and cannot be removed.");
        }

        // Remove the company association and revert the user's role to CANDIDATE.
        recruiter.setCompany(null);
        recruiter.setRole(Role.CANDIDATE);

        // Return the updated user DTO
        return userMapper.toDto(recruiter);
    }

    @Transactional
    public CompanyResponseDto changeCompanyEmployer(Long companyId, Long currentEmployerId, Long newEmployerId) {
        // Only the current employer of the company can appoint a new one.
        User currentEmployer = userService.findActiveUserById(currentEmployerId);
        Company company = findActiveCompanyById(companyId);
        authorizationService.ensureCompanyRole(currentEmployer, company, Role.EMPLOYER);

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
    public void deleteCompany(Long companyId, Long actorId) {
        // An employer can only be associated WITH ONE company they create. They can appoint MANY recruiters with their company,
        // but each recruiter CANNOT work as a recruiter to other companies. When I delete a company, I must:
        // A. disassociate All the related users (employers and recruiters)
        // B. Mark the jobs they made for the company AS DELETED (soft-delete).
        // I DON'T delete any users, I just make them a "CANDIDATE" so they can still use the normal job finding services.

        User actingUser = userService.findActiveUserById(actorId);
        Company company = findActiveCompanyById(companyId);
        authorizationService.ensureCompanyRole(actingUser, company, Role.EMPLOYER);

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
    public List<UserResponseDto> getActiveRecruitersForCompany(Long companyId, Long actorId) {

        // Only the employer of this company can view its recruiters.
        User actingUser = userService.findActiveUserById(actorId);
        Company company = findActiveCompanyById(companyId);
        authorizationService.ensureCompanyRole(actingUser,company,Role.EMPLOYER);

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
