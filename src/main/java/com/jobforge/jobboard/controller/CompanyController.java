package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.CompanyCreationDto;
import com.jobforge.jobboard.dto.CompanyResponseDto;
import com.jobforge.jobboard.dto.CompanyUpdateDto;
import com.jobforge.jobboard.dto.UserResponseDto;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /// CREATE
    @PostMapping()
    public ResponseEntity<CompanyResponseDto> createCompany(@Valid @RequestBody CompanyCreationDto companyCreationDto, @AuthenticationPrincipal CustomUserDetails principal) {
        CompanyResponseDto companyResponseDto = companyService.createCompany(companyCreationDto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(companyResponseDto);
    }


    /// POST
    @PostMapping("/myRecruiters/{recruiterId}")
    public ResponseEntity<UserResponseDto> appointRecruiter(@PathVariable Long recruiterId, @AuthenticationPrincipal CustomUserDetails principal) {

        UserResponseDto responseDto = companyService.appointRecruiter(recruiterId, principal);
        return ResponseEntity.ok(responseDto);  // HTTP 200 OK status with the newly updated user's DTO.
    }


    /// READ
    @GetMapping()
    public ResponseEntity<List<CompanyResponseDto>> getAllCompanies() {
        List<CompanyResponseDto> dtos =  companyService.getAllActiveCompanyResponseDtos();
        return ResponseEntity.status(HttpStatus.OK).body(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> getCompanyById(@PathVariable Long id) {
        CompanyResponseDto companyResponseDto = companyService.getActiveCompanyResponseDtoById(id);
        return ResponseEntity.ok(companyResponseDto);
    }

    @GetMapping("/myRecruiters")
    public ResponseEntity<List<UserResponseDto>> getRecruitersForCompany(@AuthenticationPrincipal CustomUserDetails principal) {

        List<UserResponseDto> recruiters = companyService.getActiveRecruitersForCompany(principal);

        return ResponseEntity.ok(recruiters);
    }


    /// UPDATE
    @PutMapping()
    public ResponseEntity<CompanyResponseDto> updateCompany(@Valid  @RequestBody CompanyUpdateDto updateDto, @AuthenticationPrincipal CustomUserDetails principal) {
        CompanyResponseDto responseDto = companyService.updateCompany(updateDto, principal);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/changeEmployer")
    public ResponseEntity<CompanyResponseDto> changeCompanyEmployer(@RequestParam Long newEmployerId, @AuthenticationPrincipal CustomUserDetails principal) {

        CompanyResponseDto updatedCompany = companyService.changeCompanyEmployer(newEmployerId, principal);
        return ResponseEntity.ok(updatedCompany);
    }


    /// DELETE
    @DeleteMapping()
    public ResponseEntity<Void> deleteCompany(@AuthenticationPrincipal CustomUserDetails principal) {
        companyService.deleteCompany(principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/myRecruiters/{recruiterId}")
    public ResponseEntity<UserResponseDto> removeRecruiter( @PathVariable Long recruiterId,  @AuthenticationPrincipal CustomUserDetails principal) {

        UserResponseDto responseDto = companyService.removeRecruiter(recruiterId, principal);
        return ResponseEntity.ok(responseDto);
    }
}
