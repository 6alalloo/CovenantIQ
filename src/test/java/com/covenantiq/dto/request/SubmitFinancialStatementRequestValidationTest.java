package com.covenantiq.dto.request;

import com.covenantiq.enums.PeriodType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SubmitFinancialStatementRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNegativeExtendedMonetaryFields() {
        SubmitFinancialStatementRequest request = new SubmitFinancialStatementRequest(
                PeriodType.QUARTERLY,
                2025,
                1,
                new BigDecimal("100"),
                new BigDecimal("80"),
                new BigDecimal("200"),
                new BigDecimal("120"),
                new BigDecimal("40"),
                new BigDecimal("10"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                new BigDecimal("-1"),
                OffsetDateTime.now()
        );

        assertFalse(validator.validate(request).isEmpty());
    }
}
