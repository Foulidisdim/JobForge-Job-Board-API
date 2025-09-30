package com.jobforge.jobboard.entity;

import com.jobforge.jobboard.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary key, auto incremented
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 60) //BCrypt hash limit
    private String passwordHash;

    @Column(length = 15) //international max length
    private String phoneNumber;

    @Column(length = 500)
    private String profilePictureUrl;

    // Enum is stored as STRING (e.g., "CANDIDATE") instead of ORDINAL (0,1,2) to avoid issues if roles are added later.
    // All users default to CANDIDATE; roles are updated based on user actions (e.g., creating a company).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CANDIDATE;

    // Soft delete management field
    @Column(nullable = false)
    private boolean deleted = false;

    // --Relationships--

    // EMPLOYERS/RECRUITERS -> COMPANY
    // IN THE USERS TABLE, a foreign key column named company_id will hold
    // the id (primary key) for the company the user is CURRENTLY associated with.
    // JPA automatically handles access to the related Company object, so I can access it from inside the user!!
    // That's why I declare a Company field and not a Long companyId one.
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    // CANDIDATE -> APPLICATIONS
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications;

    // EMPLOYER/RECRUITER -> JOBS
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Job> jobs;
}

