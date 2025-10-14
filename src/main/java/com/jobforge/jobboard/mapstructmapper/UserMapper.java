package com.jobforge.jobboard.mapstructmapper;

import com.jobforge.jobboard.dto.UserRegistrationDto;
import com.jobforge.jobboard.dto.UserResponseDto;
import com.jobforge.jobboard.dto.UserUpdateDetailsDto;
import com.jobforge.jobboard.entity.User;
import org.mapstruct.*;

// Eliminate boilerplate code like dto.setFirstName(user.getFirstName()) on the service layer with mapStruct!
@Mapper(componentModel = "spring") // The mapper becomes a spring bean so it can be injected
public interface UserMapper {

    // --- MAPPING FROM DTO TO ENTITY (for creation) ---
    // Maps UserRegistrationDto to User entity for account creation.
    // Ignores fields managed by the database or service layer, such as id, passwordHash, role, company, applications, jobs, and deleted.
    // These will get initialized at some point later on, based on other actions on the service layer.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "jobs", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "sessionInvalidationTime", ignore = true)
    @Mapping(target = "recoveryToken", ignore = true)
    @Mapping(target = "recoveryTokenExpirationTime", ignore = true)
    User toEntity(UserRegistrationDto userDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "applications", ignore = true)
    @Mapping(target = "jobs", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "sessionInvalidationTime", ignore = true)
    @Mapping(target = "recoveryToken", ignore = true)
    @Mapping(target = "recoveryTokenExpirationTime", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDetailsFromDto(UserUpdateDetailsDto dto, @MappingTarget User user);


    // --MAPPING FROM ENTITY TO DTO--
    UserResponseDto toDto(User user);
}