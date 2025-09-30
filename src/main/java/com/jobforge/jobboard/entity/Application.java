package com.jobforge.jobboard.entity;

import com.jobforge.jobboard.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tells JPA to rely on the database’s auto-increment feature for a unique identifier.
    private Long id;

    @Column(length = 500)
    private String resumeUrl;

    // Initialized as APPLIED on creation.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(columnDefinition = "TEXT")
    private String applicationNotes; //A feedback section from the recruiter to the candidate (e.g., For the final company decision).


    @CreatedDate //Automatic listener by JPA Auditing!
    @Column(nullable = false, updatable = false)
    private Instant appliedAt;

    @LastModifiedDate //Automatic listener by JPA Auditing!
    @Column(nullable = false)
    private Instant updatedAt; //Updated when any is edited.


    /// --Relationships--
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;
}

