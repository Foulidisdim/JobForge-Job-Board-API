package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.CompanyCreationDto;
import com.jobforge.jobboard.dto.CompanyResponseDto;
import com.jobforge.jobboard.dto.CompanyUpdateDto;
import com.jobforge.jobboard.dto.UserResponseDto;
import com.jobforge.jobboard.entity.User;
import com.jobforge.jobboard.security.CustomUserDetails;
import com.jobforge.jobboard.service.CompanyService;
import com.jobforge.jobboard.service.UserService;
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
    private final UserService userService;

    /// CREATE
    @PostMapping()
    public ResponseEntity<CompanyResponseDto> createCompany(@Valid @RequestBody CompanyCreationDto companyCreationDto, @AuthenticationPrincipal CustomUserDetails principal) {

        User founder = userService.findActiveUserById(principal.getId());
        CompanyResponseDto companyResponseDto = companyService.createCompany(companyCreationDto, founder);
        return ResponseEntity.status(HttpStatus.CREATED).body(companyResponseDto);
    }


    /// POST
    @PostMapping("/myRecruiters/{recruiterId}")
    public ResponseEntity<UserResponseDto> appointRecruiter(@PathVariable Long recruiterId, @AuthenticationPrincipal CustomUserDetails principal) {

        User userToAppoint = userService.findActiveUserById(recruiterId);
        UserResponseDto responseDto = companyService.appointRecruiter(userToAppoint, principal);
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

        User currentEmployer = userService.findActiveUserById(principal.getId());
        User newEmployer = userService.findActiveUserById(newEmployerId);
        CompanyResponseDto updatedCompany = companyService.changeCompanyEmployer(currentEmployer, newEmployer, principal);
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

        User recruiter = userService.findActiveUserById(recruiterId);
        UserResponseDto responseDto = companyService.removeRecruiter(recruiter, principal);
        return ResponseEntity.ok(responseDto);
    }
}