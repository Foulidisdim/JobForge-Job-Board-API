package com.jobforge.jobboard.mapstructmapper;

import com.jobforge.jobboard.dto.ApplicationCreationDto;
import com.jobforge.jobboard.dto.ApplicationResponseDto;
import com.jobforge.jobboard.dto.ApplicationUpdateDto;
import com.jobforge.jobboard.dto.UserResponseDto;
import com.jobforge.jobboard.entity.Application;
import com.jobforge.jobboard.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {JobMapper.class, UserMapper.class})
public interface ApplicationMapper {

    // --MAPPING FROM DTO TO ENTITY (for creation)--
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", ignore = true)  // Will get fetched and linked from the service layer.
    @Mapping(target = "candidate", ignore = true) // Will get fetched and linked from the service layer.
    @Mapping(target = "status", ignore = true) // Set as APPLIED upon creation. Changes according to candidate or employer/recruiter actions only on the service layer.
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "applicationNotes", ignore = true)
    Application toEntity(ApplicationCreationDto dto);

    // --MAPPING FROM DTO TO ENTITY (for update)--
    // Only 'status' and 'applicationNotes' are updatable; all other fields are ignored.
    // @MappingTarget tells MapStruct to MODIFY THE EXISTING APPLICATION RECORD/ENTITY rather than creating a new one.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", ignore = true)
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target ="resumeUrl", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE) // Ignore any field that is null on the UpdateDTO to not lose any data!
    void updateApplicationFromDto(ApplicationUpdateDto dto, @MappingTarget Application application); // "UPDATE the existing "application" entity you received as a parameter"

    // --MAPPING FROM ENTITY TO DTO (for response)--
    @Mapping(target = "job", source = "job") // From the Job field on the application entity that defines a Job entity, to a JobResponseDto object that is nested inside the ApplicationResponseDto.
    @Mapping(target = "candidate", source = "candidate", qualifiedByName = "optionalCandidate") // Likewise.
    ApplicationResponseDto toDto(Application entity);

    // Maps Candidate to UserResponseDto if present; returns null otherwise.
    // Prevents sending candidate details unnecessarily (e.g., when a candidate views their own applications, in which case I set the ResponseDto field as null when I build it).
    @Named("optionalCandidate")
    default UserResponseDto optionalCandidate(User candidate) {
        return candidate!=null ? new UserMapperImpl().toDto(candidate) : null;
    }
}