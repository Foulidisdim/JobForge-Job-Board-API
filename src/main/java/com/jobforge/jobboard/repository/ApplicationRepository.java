package com.jobforge.jobboard.repository;

import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.enums.ApplicationStatus;
import com.jobforge.jobboard.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByIdAndStatusIn(Long applicationId, ApplicationStatus... statuses);
    Optional<Application> findByIdAndCandidateIdAndCandidate_DeletedFalse(Long applicationId, Long candidateId);


    List<Application> findAllByCandidateIdAndCandidateDeletedFalse(Long candidateId);

    List<Application> findAllByCandidateIdAndStatusNotIn(Long candidateId, ApplicationStatus... excludedStatuses);

    List<Application> findAllByJobId(Long jobId);


// List<Application> findByStatus(ApplicationStatus status);
// List<Application> findByCandidateAndStatus(User candidate, ApplicationStatus status);

    boolean existsByJob_IdAndJob_StatusAndCandidate_IdAndCandidate_DeletedFalse(Long jobId, JobStatus jobStatus, Long candidateId);
}