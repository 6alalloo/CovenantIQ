package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.exception.UnprocessableEntityException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FinancialRatioService {

    private static final int SCALE = 6;

    public BigDecimal calculateCurrentRatio(FinancialStatement statement) {
        if (statement.getCurrentLiabilities().compareTo(BigDecimal.ZERO) == 0) {
            throw new UnprocessableEntityException("currentLiabilities cannot be zero");
        }
        return statement.getCurrentAssets().divide(statement.getCurrentLiabilities(), SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDebtToEquity(FinancialStatement statement) {
        if (statement.getTotalEquity().compareTo(BigDecimal.ZERO) == 0) {
            throw new UnprocessableEntityException("totalEquity cannot be zero");
        }
        return statement.getTotalDebt().divide(statement.getTotalEquity(), SCALE, RoundingMode.HALF_UP);
    }
}
