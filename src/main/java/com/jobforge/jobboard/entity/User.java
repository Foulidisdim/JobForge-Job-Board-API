package com.jobforge.jobboard.entity;

import com.jobforge.jobboard.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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

    /**
     * <p><strong>Security Field:</strong> Records the timestamp of the user’s most recent
     * session invalidation event — such as an explicit logout, password change, or account soft-deletion/deactivation.</p>
     *
     * <p>This field acts as a server-side safeguard for edge cases where an access token may still
     * be cryptographically valid for a short period (e.g., within its 15-minute lifetime), despite having already
     * deleted its associated refresh token. Any token issued <strong>before or at</strong> this timestamp should be rejected during authentication.</p>
     *
     * <p><strong>Why it matters:</strong><br>
     * JWTs are stateless and cannot be revoked once issued. While cryptographic signatures prevent
     * token tampering (any attempt to modify a token will fail its server side verification, unless the attacker possesses the server’s secret signing key),
     * they do not inherently handle token invalidation scenarios such as logout, password reset, or account compromise.</p>
     *
     * <p>By comparing the token’s issued-at (iat) claim against this timestamp, the system can
     * enforce server-driven session invalidation even when the token itself remains cryptographically valid.</p>
     *
     * <p><strong>Notes:</strong></p>
     * <ul>
     *   <li>This field is nullable — {@code null} indicates that no session invalidation has occurred yet.</li>
     *   <li>Any token with an issuance time prior to this timestamp is considered invalid.</li>
     *   <li>Ensures business-logic-level control over token validity, independent of JWT’s signature validation.</li>
     * </ul>
     */
    @Column
    private Instant sessionInvalidationTime;

    // Special recovery token and expiry time for password reset and soft-deleted account reinstantiation.
    @Column
    private String recoveryToken;
    @Column
    private Instant recoveryTokenExpirationTime;


    /// --Relationships--
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