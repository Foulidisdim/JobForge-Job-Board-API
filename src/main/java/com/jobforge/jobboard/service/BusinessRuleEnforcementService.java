package com.jobforge.jobboard.service;

import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.enums.Role;
import com.jobforge.jobboard.exception.UnauthorizedException;
import com.jobforge.jobboard.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessRuleEnforcementService {

    private final UserService userService;
    private final JobService jobService;


    // Checks whether a recruiter or employer can view applications for a given job
    public boolean canManageJobApplications(Job job, CustomUserDetails principal) {
        User user = userService.findActiveUserById(principal.getId());

        if (user.getCompany() == null || job.getCompany() == null)
            throw new UnauthorizedException("You are not associated with any company.");

        if (!job.getCompany().getId().equals(user.getCompany().getId()))
            throw new UnauthorizedException("You are not allowed to manage this job’s applications.");

        if (user.getRole() != Role.EMPLOYER && user.getRole() != Role.RECRUITER)
            throw new UnauthorizedException("Only employers or recruiters can manage job applications.");

        return true;
    }

    // Ensures the logged-in candidate is the owner of the application
    public boolean isApplicationOwner(Application application, CustomUserDetails principal) {
        return application.getCandidate().getId().equals(principal.getId());
    }
}