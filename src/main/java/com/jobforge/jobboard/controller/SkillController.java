package com.jobforge.jobboard.controller;

import com.jobforge.jobboard.dto.SkillCreationDto;
import com.jobforge.jobboard.dto.SkillResponseDto;
import com.jobforge.jobboard.dto.SkillUpdateDto;
import com.jobforge.jobboard.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    /// CREATE
    @PostMapping("/create")
    public ResponseEntity<SkillResponseDto> createSkill(@Valid @RequestBody SkillCreationDto skillDto) {
        SkillResponseDto createdSkill = skillService.createSkill(skillDto);
        return new ResponseEntity<>(createdSkill, HttpStatus.CREATED);
    }


    /// READ
    @GetMapping("/all")
    public ResponseEntity<List<SkillResponseDto>> getAllSkills() {
        List<SkillResponseDto> skills = skillService.findAllSkills();
        return ResponseEntity.ok(skills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponseDto> getSkillById(@PathVariable Long id) {
        SkillResponseDto skill = skillService.getResponseDtoById(id);
        return ResponseEntity.ok(skill);
    }


    /// UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<SkillResponseDto> updateSkill(@PathVariable("id") Long skillId, @Valid @RequestBody SkillUpdateDto skillUpdateDto) {

        SkillResponseDto responseDto = skillService.updateSkill(skillId, skillUpdateDto);
        return ResponseEntity.ok(responseDto);
    }


    /// DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}
