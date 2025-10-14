package com.jobforge.jobboard.entity;

import com.jobforge.jobboard.enums.EmploymentType;
import com.jobforge.jobboard.enums.ExperienceLevel;
import com.jobforge.jobboard.enums.JobStatus;
import com.jobforge.jobboard.enums.WorkArrangement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description; //The job details

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Column
    @Enumerated(EnumType.STRING)
    private WorkArrangement workArrangement;

    @Column (nullable = false)
    private Double salaryMin;

    @Column
    private Double salaryMax;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Column
    private Instant repostedAt;

    // State management field (incl. soft-delete when status is DELETED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status; // frondEnd will set DRAFT or ACTIVE upon creation ("Save as draft" or "Post now" functionality)

    //Relationships

    // Skills are stored as a separate entity instead of a comma-separated string.
    // Many-to-many relationship between jobs and skills enables effective skill searching.
    // @JoinTable creates a NEW table with two foreign keys: job_id and skill_id.
    // Job is the OWNING SIDE of the relationship. Look for @ManyToMany(mappedBy = "skills") on the Job field in the Skill entity (the NON-OWNING side)
    @ManyToMany
    @JoinTable(
            name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> skills = new ArrayList<>();


    /// --Relationships--
    // JOB -> APPLICATIONS
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<Application> applications;

    // JOBS -> EMPLOYER/RECRUITER (who created them)
    // Associated company is derived from the user at higher layers to maintain the user as a SINGLE SOURCE OF TRUTH!
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}