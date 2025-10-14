package com.jobforge.jobboard.service;

import com.jobforge.jobboard.SkillNormalizer;
import com.jobforge.jobboard.dto.SkillCreationDto;
import com.jobforge.jobboard.dto.SkillResponseDto;
import com.jobforge.jobboard.dto.SkillUpdateDto;
import com.jobforge.jobboard.entity.Skill;
import com.jobforge.jobboard.exception.DuplicateResourceException;
import com.jobforge.jobboard.exception.ResourceNotFoundException;
import com.jobforge.jobboard.mapstructmapper.SkillMapper;
import com.jobforge.jobboard.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;

    private final SkillMapper skillMapper;

    /// CREATE
    // Everyone can create skills (users will be able to put them on their profiles later)
    @Transactional
    public SkillResponseDto createSkill(SkillCreationDto skillDto) {

        // Limit duplicate entries
        // Normalize the name
        String normalizedName = SkillNormalizer.normalize(skillDto.getName());

        // Check for an existing skill with the normalized name
        Optional<Skill> existingSkill = skillRepository.findByName(normalizedName);

        // Check for existing skill
        if (existingSkill.isPresent()) {
            // You can throw an error or return the existing DTO.
            // Throwing an error is better so the user gets informed.
            throw new DuplicateResourceException("A skill with this name already exists.");
        }

        // If no duplicate is found, proceed with creation.
        Skill newSkill = new Skill();
        newSkill.setName(normalizedName);

        Skill savedSkill = skillRepository.save(newSkill);
        return skillMapper.toDto(savedSkill);
    }


    /// GET
    @Transactional(readOnly = true)
    public List<SkillResponseDto> findAllSkills() {
        return skillRepository.findAll().stream()
                .map(skillMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SkillResponseDto getResponseDtoById(Long id) {
        Skill skill = findById(id);
        return skillMapper.toDto(skill);
    }


    /// UPDATE
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SkillResponseDto updateSkill(Long skillId, SkillUpdateDto dto) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));

        skillMapper.updateSkillFromDto(dto, skill); // If the name comes as null, no changes happen.

        return skillMapper.toDto(skill); // JPA dirty checking saves the changes automatically.
    }


    /// DELETE
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill not found.");
        }
        skillRepository.deleteById(id);
    }

    /// Internal service-to-service data exchange methods (separation of concerns)
    public Skill findById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found."));
    }
}