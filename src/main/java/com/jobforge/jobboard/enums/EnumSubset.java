package com.jobforge.jobboard.enums;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation to ensure an enum field's value is one of the specified allowed values.
 *
 * <p>Example: To allow only {@code JobStatus.DRAFT} or {@code JobStatus.ACTIVE}, use it like this:
 * <pre>{@code @EnumSubset(anyOf = {"DRAFT", "ACTIVE"})}</pre>
 *
 * <p>This annotation is powered by the {@link EnumSubsetValidator},
 * which implements the validation logic.
 *
 * @see com.jobforge.jobboard.enums.EnumSubsetValidator
 */

@Documented // Makes this annotation appear in the generated Javadoc for any class that uses it.
@Constraint(validatedBy = EnumSubsetValidator.class) // Links this annotation to its corresponding validator class.
@Target({ FIELD, PARAMETER}) // Specifies it can validate FIELDS AND METHOD PARAMETERS.
@Retention(RUNTIME) // Specifies it's required on RUNTIME for the validation framework to process it.
public @interface EnumSubset {
    String message() default "Value is not allowed."; // Default error message if validation fails.
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    // The validator can apply to any class that is a subclass of Enum, i.e., any enum type (Like JobStatus or ApplicationStatus).
    Class<? extends Enum<?>> enumClass();

    // This is the custom attribute for allowed enum values. An array of strings containing the allowed ENUM values.
    String[] anyOf();
}