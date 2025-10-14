package com.jobforge.jobboard.mapstructmapper;

import com.jobforge.jobboard.dto.JobCreationDto;
import com.jobforge.jobboard.dto.JobResponseDto;
import com.jobforge.jobboard.dto.JobUpdateDto;
import com.jobforge.jobboard.entity.Job;
import org.mapstruct.*;


@Mapper(componentModel = "spring", uses = {CompanyMapper.class, SkillMapper.class}) //nested mappers for skill and company fields on the jobCreationDto
public interface JobMapper {

    // --MAPPING FROM DTO TO ENTITY--
    // The corresponding Skills and applications will need to be fetched and set to the entity by the service layer.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "managedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "repostedAt", ignore = true)
    Job toEntity(JobCreationDto jobCreateDto);

    // --MAPPING FROM DTO TO ENTITY (for updates) --
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "managedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "repostedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateJobFromDto(JobUpdateDto dto, @MappingTarget Job entity);

    // --MAPPING FROM ENTITY TO DTO--
    // The ResponseDto contains nested DTOs (e.g., CompanyResponseDto, SkillResponseDto).
    // MapStruct automatically maps the associated entities to their respective nested DTOs with the mappers on the uses = {CompanyMapper.class, SkillMapper.class} parameter.
    JobResponseDto toDto(Job job);
}