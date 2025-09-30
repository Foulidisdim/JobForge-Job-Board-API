package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.CompanyCreationDto;
import com.jobforge.jobboard.dto.CompanyResponseDto;
import com.jobforge.jobboard.dto.CompanyUpdateDto;
import com.jobforge.jobboard.dto.UserResponseDto;
import com.jobforge.jobboard.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // TODO: Change the temporary request params i use to test functionality with the JWT authentication principal after the implementation.

    /// CREATE
    @PostMapping("/create")
    public ResponseEntity<CompanyResponseDto> createCompany(@Valid @RequestBody CompanyCreationDto companyCreationDto, @RequestParam Long userId) {
        CompanyResponseDto companyResponseDto = companyService.createCompany(companyCreationDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(companyResponseDto);
    }


    /// POST
    @PostMapping("/{companyId}/appointRecruiter/{recruiterId}")
    public ResponseEntity<UserResponseDto> appointRecruiter(@PathVariable Long companyId, @PathVariable Long recruiterId, @RequestParam Long actorId) {

        UserResponseDto responseDto = companyService.appointRecruiter(companyId, recruiterId, actorId);
        return ResponseEntity.ok(responseDto);  // HTTP 200 OK status with the newly updated user's DTO.
    }


    /// READ
    @GetMapping("/all")
    public ResponseEntity<List<CompanyResponseDto>> getAllCompanies() {
        List<CompanyResponseDto> dtos =  companyService.getAllActiveCompanyResponseDtos();
        return ResponseEntity.status(HttpStatus.OK).body(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> getCompanyById(@PathVariable Long id) {
        CompanyResponseDto companyResponseDto = companyService.getActiveCompanyResponseDtoById(id);
        return ResponseEntity.ok(companyResponseDto);
    }

    @GetMapping("/{id}/recruiters")
    public ResponseEntity<List<UserResponseDto>> getRecruitersForCompany(@PathVariable Long id, @RequestParam Long actorId) {

        List<UserResponseDto> recruiters = companyService.getActiveRecruitersForCompany(id, actorId);

        return ResponseEntity.ok(recruiters);
    }


    /// UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> updateCompany(@PathVariable Long id, @Valid  @RequestBody CompanyUpdateDto updateDto, @RequestParam Long actorId) {
        CompanyResponseDto responseDto = companyService.updateCompany(updateDto, id, actorId);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/{companyId}/changeEmployer")
    public ResponseEntity<CompanyResponseDto> changeCompanyEmployer(@PathVariable Long companyId, @RequestParam Long currentEmployerId,
                                                                    @RequestParam Long newEmployerId) {

        CompanyResponseDto updatedCompany = companyService.changeCompanyEmployer(companyId, currentEmployerId, newEmployerId);
        return ResponseEntity.ok(updatedCompany);
    }


    /// DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id, @RequestParam Long actorId) {
        companyService.deleteCompany(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{companyId}/removeRecruiter/{recruiterId}")
    public ResponseEntity<UserResponseDto> removeRecruiter(@PathVariable Long companyId, @PathVariable Long recruiterId,  @RequestParam Long actorId) {

        UserResponseDto responseDto = companyService.removeRecruiter(companyId, recruiterId, actorId);
        return ResponseEntity.ok(responseDto);
    }
}
