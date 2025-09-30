package com.jobforge.jobboard.customValidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure a field contains a valid ISO 4217 currency code.
 *
 * <p>This annotation can be applied to {@link String} fields representing currency codes,
 * and will be validated against the official ISO 4217 list (e.g., "USD", "EUR", "JPY").
 *
 * <p>Invalid values will trigger a validation error with the default message
 * {@code "Invalid ISO 4217 currency code"}, unless overridden with a custom message.
 *
 * <p>This annotation works in conjunction with {@link CurrencyCodeValidator}, which contains
 * the logic for checking validity.
 *
 * @see CurrencyCodeValidator
 */

@Documented
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {
    String message() default "Invalid ISO 4217 currency code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}