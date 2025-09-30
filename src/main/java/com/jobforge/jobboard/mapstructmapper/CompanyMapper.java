package com.jobforge.jobboard.mapstructmapper;

import com.jobforge.jobboard.dto.CompanyCreationDto;
import com.jobforge.jobboard.dto.CompanyResponseDto;
import com.jobforge.jobboard.dto.CompanyUpdateDto;
import com.jobforge.jobboard.entity.Company;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    // --- MAPPING FROM DTO TO ENTITY (for creation) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobs", ignore = true)
    @Mapping(target = "relatedUsers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true) // Creator ID is MANUALLY set on the service.
    Company toEntity(CompanyCreationDto dto);

    // --- MAPPING FROM DTO TO ENTITY (for update) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobs", ignore = true)
    @Mapping(target = "relatedUsers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdByUserId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCompanyFromDto(CompanyUpdateDto dto, @MappingTarget Company company);

    // --- MAPPING FROM ENTITY TO DTO (for response) ---
    CompanyResponseDto toDto(Company entity);
}
