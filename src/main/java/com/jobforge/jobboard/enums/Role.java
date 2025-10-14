package com.jobforge.jobboard.enums;

public enum Role {
    CANDIDATE, // Jobseekers (can search and apply for jobs, view their applications)
    RECRUITER, // Working on behalf of a company to handle job postings (can create, update and delete job postings of the companies they are related with).
    EMPLOYER, // Company profile owner (create/update companies and job postings of such companies)
    ADMIN // Platform administrator
}