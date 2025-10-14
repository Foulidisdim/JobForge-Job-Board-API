package com.jobforge.jobboard.repository;

import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByIdAndStatus(Long id, JobStatus status);
    Optional<Job> findByIdAndStatusNot(Long id, JobStatus status);


    //Admin: Finding soft-deleted jobs. Others: Finding Drafted, Active and Closed Jobs.
    List<Job> findAllByStatus(JobStatus jobStatus);

    List<Job> findAllByCompanyIdAndStatus(Long companyId, JobStatus jobStatus);

    List<Job> findAllByManagedByIdAndCompanyId(Long jobManagerId, Long companyId);
}