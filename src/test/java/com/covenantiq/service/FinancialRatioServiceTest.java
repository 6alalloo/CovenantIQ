package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.enums.CovenantType;
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

        assertEquals(new BigDecimal("1.5000"), ratio);
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

        assertEquals(new BigDecimal("2.5000"), ratio);
    }

    @Test
    void calculatesAllPhase2Ratios() {
        FinancialStatement statement = new FinancialStatement();
        statement.setCurrentAssets(new BigDecimal("1000"));
        statement.setCurrentLiabilities(new BigDecimal("400"));
        statement.setTotalDebt(new BigDecimal("1200"));
        statement.setTotalEquity(new BigDecimal("600"));
        statement.setEbit(new BigDecimal("300"));
        statement.setInterestExpense(new BigDecimal("100"));
        statement.setNetOperatingIncome(new BigDecimal("500"));
        statement.setTotalDebtService(new BigDecimal("250"));
        statement.setIntangibleAssets(new BigDecimal("150"));
        statement.setEbitda(new BigDecimal("400"));
        statement.setFixedCharges(new BigDecimal("50"));
        statement.setInventory(new BigDecimal("300"));
        statement.setTotalAssets(new BigDecimal("5000"));
        statement.setTotalLiabilities(new BigDecimal("1800"));

        assertEquals(new BigDecimal("2.5000"), financialRatioService.calculateByCovenantType(CovenantType.CURRENT_RATIO, statement));
        assertEquals(new BigDecimal("2.0000"), financialRatioService.calculateByCovenantType(CovenantType.DEBT_TO_EQUITY, statement));
        assertEquals(new BigDecimal("2.0000"), financialRatioService.calculateByCovenantType(CovenantType.DSCR, statement));
        assertEquals(new BigDecimal("3.0000"), financialRatioService.calculateByCovenantType(CovenantType.INTEREST_COVERAGE, statement));
        assertEquals(new BigDecimal("3050.0000"), financialRatioService.calculateByCovenantType(CovenantType.TANGIBLE_NET_WORTH, statement));
        assertEquals(new BigDecimal("3.0000"), financialRatioService.calculateByCovenantType(CovenantType.DEBT_TO_EBITDA, statement));
        assertEquals(new BigDecimal("2.3333"), financialRatioService.calculateByCovenantType(CovenantType.FIXED_CHARGE_COVERAGE, statement));
        assertEquals(new BigDecimal("1.7500"), financialRatioService.calculateByCovenantType(CovenantType.QUICK_RATIO, statement));
    }

    @Test
    void throwsWhenDscrDenominatorZero() {
        FinancialStatement statement = new FinancialStatement();
        statement.setNetOperatingIncome(new BigDecimal("200"));
        statement.setTotalDebtService(BigDecimal.ZERO);

        assertThrows(UnprocessableEntityException.class, () -> financialRatioService.calculateDSCR(statement));
    }
}
