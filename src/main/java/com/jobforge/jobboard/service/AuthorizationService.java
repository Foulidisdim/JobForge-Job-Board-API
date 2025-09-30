package com.jobforge.jobboard.service;

import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.Company;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    ///Centralized service for all authorization operations on the service layer!

    /// -- Basic self/admin checks --
    // Ensure the actor is the same as the resource owner.
    public void ensureSelf(Long actorId, Long resourceOwnerId) {
        if (!Objects.equals(actorId, resourceOwnerId)) {
            throw new UnauthorizedException("You can only perform this action on your own resource.");
        }
    }

    // Ensure the user has admin privileges.
    public void ensureAdmin(User actor) {
        if (actor.getRole() != Role.ADMIN) throw new UnauthorizedException("Only an admin can perform this action.");
    }

    // Ensure the actor is the same as the resource owner OR has admin privileges.
    public void ensureSelfOrAdmin(User actor, Long resourceOwnerId) {
        if (actor.getRole() == Role.ADMIN) return;
        if (!(Objects.equals(actor.getId(), resourceOwnerId))) {
            throw new UnauthorizedException("Not authorized for this action.");
        }
    }

    // APPLICATION SERVICE: Ensure the actor is the application's candidate.
    public void ensureCandidateSelf(Long actorId, Long candidateId) {
        if (!Objects.equals(actorId, candidateId)) {
            throw new UnauthorizedException("You can only perform this action on your own applications.");
        }
    }

    /// -- Company personnel & Role checks --
    // Ensure the user has one of the allowed roles.
    //Role...: Variable-length Role arguments! I can call the method with zero, multiple, or an array of Role objects!
    public void ensureRole(User actor, Role... allowedRoles) {
        for (Role role : allowedRoles) { // Treated as an array or Role objects.
            if (actor.getRole() == role) return;
        }

        String rolesString = Arrays.stream(allowedRoles)
                .map(Enum::toString)
                .collect(Collectors.joining(", "));
        throw new UnauthorizedException("You do not have the required role to perform this action. Required roles: " + rolesString);
    }

    // Ensure the actor belongs to the given company.
    public void ensureUserBelongsToCompany(User actor, Company company) {
        if (actor.getCompany() == null || !actor.getCompany().equals(company)) {
            throw new UnauthorizedException("You are not associated with this company.");
        }
    }

    // Ensure the actor has one of the allowed roles and belongs to the company.
    // Combine role and company checks for maximum reusability.
    public void ensureCompanyRole(User actor, Company company, Role... allowedRoles) {
        ensureUserBelongsToCompany(actor, company);
        ensureRole(actor, allowedRoles);
    }

    /// -- Application access checks --
    public void ensureApplicationAccess(User actor, Application application) {
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

