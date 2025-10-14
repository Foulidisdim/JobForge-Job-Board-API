package com.jobforge.jobboard.repository;

import com.jobforge.jobboard.entity.Company;
import com.jobforge.jobboard.entity.Job;
import com.jobforge.jobboard.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByIdAndStatusNot(Long id, JobStatus deletedStatus);

    //Admin: Finding soft-deleted jobs. Others: Finding Drafted, Active and Closed Jobs.
    List<Job> findAllByStatus(JobStatus jobStatus);

    List<Job> findAllByCompanyAndStatus(Company company, JobStatus jobStatus);
}