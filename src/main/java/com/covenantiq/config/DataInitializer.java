package com.covenantiq.config;

import com.covenantiq.dto.request.CreateCovenantRequest;
import com.covenantiq.dto.request.CreateLoanRequest;
import com.covenantiq.dto.request.SubmitFinancialStatementRequest;
import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.PeriodType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.LoanRepository;
import com.covenantiq.service.CovenantService;
import com.covenantiq.service.FinancialStatementService;
import com.covenantiq.service.LoanService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedData(
            LoanRepository loanRepository,
            LoanService loanService,
            CovenantService covenantService,
            FinancialStatementService financialStatementService
    ) {
        return args -> {
            if (loanRepository.count() > 0) {
                return;
            }

            Long loanId = loanService.createLoan(new CreateLoanRequest(
                    "Acme Manufacturing LLC",
                    new BigDecimal("5000000.00"),
                    LocalDate.of(2025, 1, 15)
            )).getId();

            covenantService.createCovenant(loanId, new CreateCovenantRequest(
                    CovenantType.CURRENT_RATIO,
                    new BigDecimal("1.20"),
                    ComparisonType.GREATER_THAN_EQUAL,
                    SeverityLevel.HIGH
            ));
            covenantService.createCovenant(loanId, new CreateCovenantRequest(
                    CovenantType.DEBT_TO_EQUITY,
                    new BigDecimal("2.50"),
                    ComparisonType.LESS_THAN_EQUAL,
                    SeverityLevel.MEDIUM
            ));

            financialStatementService.submitStatement(loanId, new SubmitFinancialStatementRequest(
                    PeriodType.QUARTERLY,
                    2025,
                    1,
                    new BigDecimal("2500000"),
                    new BigDecimal("1500000"),
                    new BigDecimal("6000000"),
                    new BigDecimal("3000000"),
                    new BigDecimal("500000"),
                    new BigDecimal("120000"),
                    OffsetDateTime.of(2025, 3, 30, 12, 0, 0, 0, ZoneOffset.UTC)
            ));

            financialStatementService.submitStatement(loanId, new SubmitFinancialStatementRequest(
                    PeriodType.QUARTERLY,
                    2025,
                    2,
                    new BigDecimal("2300000"),
                    new BigDecimal("1600000"),
                    new BigDecimal("6200000"),
                    new BigDecimal("2800000"),
                    new BigDecimal("460000"),
                    new BigDecimal("130000"),
                    OffsetDateTime.of(2025, 6, 30, 12, 0, 0, 0, ZoneOffset.UTC)
            ));

            financialStatementService.submitStatement(loanId, new SubmitFinancialStatementRequest(
                    PeriodType.QUARTERLY,
                    2025,
                    3,
                    new BigDecimal("2100000"),
                    new BigDecimal("1700000"),
                    new BigDecimal("6400000"),
                    new BigDecimal("2500000"),
                    new BigDecimal("430000"),
                    new BigDecimal("140000"),
                    OffsetDateTime.of(2025, 9, 30, 12, 0, 0, 0, ZoneOffset.UTC)
            ));
        };
    }
}
