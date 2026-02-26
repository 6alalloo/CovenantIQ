package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.exception.UnprocessableEntityException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FinancialRatioService {

    private static final int SCALE = 4;

    public BigDecimal calculateByCovenantType(CovenantType covenantType, FinancialStatement statement) {
        return switch (covenantType) {
            case CURRENT_RATIO -> calculateCurrentRatio(statement);
            case DEBT_TO_EQUITY -> calculateDebtToEquity(statement);
            case DSCR -> calculateDSCR(statement);
            case INTEREST_COVERAGE -> calculateInterestCoverage(statement);
            case TANGIBLE_NET_WORTH -> calculateTangibleNetWorth(statement);
            case DEBT_TO_EBITDA -> calculateDebtToEbitda(statement);
            case FIXED_CHARGE_COVERAGE -> calculateFixedChargeCoverage(statement);
            case QUICK_RATIO -> calculateQuickRatio(statement);
        };
    }

    public BigDecimal calculateCurrentRatio(FinancialStatement statement) {
        return divide(
                require(statement.getCurrentAssets(), "currentAssets"),
                require(statement.getCurrentLiabilities(), "currentLiabilities"),
                "currentLiabilities"
        );
    }

    public BigDecimal calculateDebtToEquity(FinancialStatement statement) {
        return divide(
                require(statement.getTotalDebt(), "totalDebt"),
                require(statement.getTotalEquity(), "totalEquity"),
                "totalEquity"
        );
    }

    public BigDecimal calculateDSCR(FinancialStatement statement) {
        return divide(
                require(statement.getNetOperatingIncome(), "netOperatingIncome"),
                require(statement.getTotalDebtService(), "totalDebtService"),
                "totalDebtService"
        );
    }

    public BigDecimal calculateInterestCoverage(FinancialStatement statement) {
        return divide(
                require(statement.getEbit(), "ebit"),
                require(statement.getInterestExpense(), "interestExpense"),
                "interestExpense"
        );
    }

    public BigDecimal calculateTangibleNetWorth(FinancialStatement statement) {
        return require(statement.getTotalAssets(), "totalAssets")
                .subtract(require(statement.getIntangibleAssets(), "intangibleAssets"))
                .subtract(require(statement.getTotalLiabilities(), "totalLiabilities"))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDebtToEbitda(FinancialStatement statement) {
        return divide(
                require(statement.getTotalDebt(), "totalDebt"),
                require(statement.getEbitda(), "ebitda"),
                "ebitda"
        );
    }

    public BigDecimal calculateFixedChargeCoverage(FinancialStatement statement) {
        BigDecimal fixedCharges = require(statement.getFixedCharges(), "fixedCharges");
        BigDecimal interestExpense = require(statement.getInterestExpense(), "interestExpense");
        BigDecimal numerator = require(statement.getEbit(), "ebit").add(fixedCharges);
        BigDecimal denominator = fixedCharges.add(interestExpense);
        return divide(numerator, denominator, "fixedCharges + interestExpense");
    }

    public BigDecimal calculateQuickRatio(FinancialStatement statement) {
        BigDecimal numerator = require(statement.getCurrentAssets(), "currentAssets")
                .subtract(require(statement.getInventory(), "inventory"));
        return divide(numerator, require(statement.getCurrentLiabilities(), "currentLiabilities"), "currentLiabilities");
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator, String denominatorName) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            throw new UnprocessableEntityException(denominatorName + " cannot be zero");
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal require(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new UnprocessableEntityException(fieldName + " is required for ratio calculation");
        }
        return value;
    }
}
