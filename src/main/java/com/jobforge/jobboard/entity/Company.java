package com.jobforge.jobboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "companies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"employer", "relatedUsers", "jobs"})
@EqualsAndHashCode(exclude = {"employer", "relatedUsers", "jobs"})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // Essential to define an SQL TEXT type column by JPA.
    // Not String or VARCHAR, which has a small character limit.
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String industry;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    // Soft delete management field
    @Column(nullable = false)
    private boolean deleted = false;


    /// --Relationships--
    // COMPANY -> JOBS
    // mappedBy = "company" sets a foreign key ON THE JOB ENTITY that holds the COMPANY THAT POSTED THE JOB. Will specify that foreign key column name on the Job Entity
    // Look for @JoinColumn(name = "company_id").
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true) // cascade ALL: if a company is deleted, the jobs are deleted too.
    private List<Job> jobs;

    // COMPANY -> EMPLOYERS/RECRUITERS (Employer AND recruiters associated with the company)
    @OneToMany(mappedBy = "company")
    private List<User> relatedUsers;

    // TODO: Rethink nullable enforcement when making the admin hard delete operations. Some fields have to be nullable if data is actually deleted.
    @ManyToOne
    @JoinColumn(name = "employer_id", nullable = false)
    private User employer;
}