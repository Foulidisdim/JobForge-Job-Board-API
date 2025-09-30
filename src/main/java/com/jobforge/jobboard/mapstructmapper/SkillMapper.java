package com.jobforge.jobboard.mapstructmapper;

import com.jobforge.jobboard.dto.SkillCreationDto;
import com.jobforge.jobboard.dto.SkillResponseDto;
import com.jobforge.jobboard.dto.SkillUpdateDto;
import com.jobforge.jobboard.entity.Skill;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    // --- MAPPING FROM DTO TO ENTITY ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobs", ignore = true)
    Skill toEntity(SkillCreationDto skillCreationDto);

    // --- MAPPING FROM ENTITY TO DTO ---
    // When retrieving A SINGLE skill from the DB
    SkillResponseDto toDto(Skill entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true) // never update the id
    void updateSkillFromDto(SkillUpdateDto dto, @MappingTarget Skill skill);
}