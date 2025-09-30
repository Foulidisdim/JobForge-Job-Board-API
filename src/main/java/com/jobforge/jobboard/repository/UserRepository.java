package com.jobforge.jobboard.repository;

import com.jobforge.jobboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> { // Repository for User entity; primary key is of type Long

    // Minding the soft delete tag, The search will either return a user or WON'T FIND ONE so it will return null.
    // "Optional" type needed to avoid null pointer exceptions (will return an Optional<User> object if the search doesn't find the required User object).
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByEmailAndDeletedTrue(String email);
    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndDeletedFalse(Long id);


    // When a list of something is expected to return (e.g., a list of users),
    // Optional ISN'T NEEDED! Spring will just return an empty list, but not a null!
    List<User> findAllByDeletedFalse();

//    List<User> findByCompanyIdAndRoleAndIsDeletedFalse(Long companyId, Role role);
//    List<User> findByCompanyIdAndIsDeletedFalse(Long companyId);
//    List<User> findByFirstNameContainingIgnoreCaseAndIsDeletedFalse(String firstName);
//    List<User> findByLastNameContainingIgnoreCaseAndIsDeletedFalse(String lastName);
//    List<User> findByRoleAndIsDeletedFalse(Role role);
}
