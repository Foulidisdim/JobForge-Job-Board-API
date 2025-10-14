package com.jobforge.jobboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /// --Relationships--
    // Bidirectional Relationship with jobs that allows to navigate from a skill to all jobs that require it (e.g., while searching by skill).
    @ManyToMany(mappedBy = "skills")
    private Set<Job> jobs;
}