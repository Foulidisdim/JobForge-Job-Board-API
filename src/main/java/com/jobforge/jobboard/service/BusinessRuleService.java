package com.jobforge.jobboard.service;

import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.UnauthorizedException;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BusinessRuleService {

    private final RefreshTokenService refreshTokenService;

    // Checks whether a recruiter or employer can view applications for a given job
    // Parameters: 1. The job posting's COMPANY id, 2. The authenticated principal.
    public void canManageJobApplications(Long jobPostingCompanyId, CustomUserDetails principal) {

        // Shouldn't happen based on the @PreAuthorize role check and business logic.
        if (principal.getCompany() == null)
            throw new UnauthorizedException("You are not associated with any company.");

        if (!Objects.equals(jobPostingCompanyId, principal.getCompany().getId()))
            throw new UnauthorizedException("You are not allowed to access this job’s applications.");
    }

    // Ensures the logged-in candidate is the owner of the application
    public void isApplicationOwner(Long candidateId, CustomUserDetails principal) {

        if(!Objects.equals(candidateId, principal.getId()))
            throw new UnauthorizedException("You can only access your own applications.");
    }

    // Ensure the logged-in principal belongs to the given company.
    public void ensureUserBelongsToCompany(Long companyId, CustomUserDetails principal) {

        if (principal.getCompany() == null || !Objects.equals(principal.getCompany().getId(), companyId))
            throw new UnauthorizedException("You are not associated with this company.");
    }

    public void ensureApplicationAccess(Application application, CustomUserDetails principal) {

        // Case 1: The actor is an admin.
        if (principal.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(Role.ADMIN.name()))) {
            return;
        }

        // Case 2: The actor is the candidate who owns the application.
        if (Objects.equals(principal.getId(), application.getCandidate().getId())) {
            return;
        }

        // Case 3: The actor is a recruiter or employer from the same company that owns the job.
        if (principal.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(Role.EMPLOYER.name()) ||
                                authority.getAuthority().equals(Role.RECRUITER.name()))){
            if (Objects.equals(principal.getCompany().getId(), application.getJob().getCompany().getId())) {
                return;
            }
        }

        // If none of the above conditions are met, throw an exception.
        throw new UnauthorizedException("You do not have permission to view this application.");
    }

    // Invalidate both session and long-term tokens (access & refresh) to force a new login with the password.
    public void invalidateAllAuthenticationTokens(User user){
        refreshTokenService.deleteTokenByUserId(user.getId());
        user.setSessionInvalidationTime(Instant.now());
    }
}