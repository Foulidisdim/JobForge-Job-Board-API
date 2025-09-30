package com.jobforge.jobboard.enums;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Validation logic for the EnumSubset custom annotation/validator. Needed to validate accepted ENUMS on a case by case basis.

// Implementing the validation interface so Spring knows this is a validator.
// EnumSubset is the custom annotation, telling that this validator handles fields annotated with @EnumSubset
// Enum<?> means Handle ENUMS of ANY type. We built a general validator for ENUMS!
public class EnumSubsetValidator implements ConstraintValidator<EnumSubset, Enum<?>> {

    private List<String> subset;

    // Initialize is a constraintValidator interface method called before validation begins.
    // We override it with the annotation's values
    @Override
    public void initialize(EnumSubset constraint) {
        this.subset = Arrays.stream(constraint.anyOf())
                .collect(Collectors.toList());
    }

    // This method contains the actual validation logic.
    // Enum<?> value contains the value of the ENUM field that awaits validation (it's of type JobStatus ACTIVE, for example).
    @Override
    public boolean isValid(Enum<?> value, ConstraintValidatorContext context) {

        if (value == null) {
            return true; // Null values should be handled by @NotNull (Don't throw an exception from here, let the NotNull handle it)
        }
        String enumValue = value.name(); //EXTRACTING THE "ACTIVE" part from the value which is, for example, JobStatus.ACTIVE
        return subset.contains(enumValue); //Boolean. Checks if the value is permitted by checking the permitted STRING values passed on the annotation.
    }
}