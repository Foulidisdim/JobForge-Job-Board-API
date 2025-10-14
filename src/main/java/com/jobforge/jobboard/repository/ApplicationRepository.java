package com.jobforge.jobboard.repository;

import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
// Optional<Application> findByJobAndCandidate(Job job, User candidate);

// List<Application> findByJobId(Long jobId);
    List<Application> findByCandidateId(Long candidateId);



    List<Application> findByJob(Job job);
// List<Application> findByStatus(ApplicationStatus status);
// List<Application> findByCandidateAndStatus(User candidate, ApplicationStatus status);

    boolean existsByJobAndCandidate(Job job, User candidate);
}