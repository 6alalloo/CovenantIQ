package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.exception.UnprocessableEntityException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinancialRatioServiceTest {

    private final FinancialRatioService financialRatioService = new FinancialRatioService();

    @Test
    void calculatesCurrentRatioWithScale() {
        FinancialStatement statement = new FinancialStatement();
        statement.setCurrentAssets(new BigDecimal("210"));
        statement.setCurrentLiabilities(new BigDecimal("140"));

        BigDecimal ratio = financialRatioService.calculateCurrentRatio(statement);

        assertEquals(new BigDecimal("1.500000"), ratio);
    }

    @Test
    void throwsWhenCurrentLiabilitiesZero() {
        FinancialStatement statement = new FinancialStatement();
        statement.setCurrentAssets(new BigDecimal("100"));
        statement.setCurrentLiabilities(BigDecimal.ZERO);

        assertThrows(UnprocessableEntityException.class, () -> financialRatioService.calculateCurrentRatio(statement));
    }

    @Test
    void calculatesDebtToEquity() {
        FinancialStatement statement = new FinancialStatement();
        statement.setTotalDebt(new BigDecimal("500"));
        statement.setTotalEquity(new BigDecimal("200"));

        BigDecimal ratio = financialRatioService.calculateDebtToEquity(statement);

        assertEquals(new BigDecimal("2.500000"), ratio);
    }
}
