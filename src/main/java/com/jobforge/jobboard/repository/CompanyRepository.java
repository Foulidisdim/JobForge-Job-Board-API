package com.jobforge.jobboard.repository;

import com.jobforge.jobboard.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByIdAndDeletedFalse(Long id);

    List<Company> findAllByDeletedFalse();
//    List<Company> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name);
//    List<Company> findByIndustryContainingIgnoreCaseAndIsDeletedFalse(String industry);

}
