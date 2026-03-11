package com.covenantiq.bootstrap.seed;

import com.covenantiq.config.AppModeProperties;
import com.covenantiq.domain.Alert;
import com.covenantiq.domain.Comment;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.domain.UserAccount;
import com.covenantiq.dto.request.CreateCovenantRequest;
import com.covenantiq.dto.request.CreateLoanRequest;
import com.covenantiq.dto.request.SubmitFinancialStatementRequest;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.ComparisonType;
import com.covenantiq.enums.CovenantType;
import com.covenantiq.enums.PeriodType;
import com.covenantiq.enums.SeverityLevel;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CommentRepository;
import com.covenantiq.repository.CovenantResultRepository;
import com.covenantiq.repository.LoanRepository;
import com.covenantiq.repository.UserAccountRepository;
import com.covenantiq.service.CovenantService;
import com.covenantiq.service.FinancialStatementService;
import com.covenantiq.service.LoanService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final List<LoanSeedProfile> LOAN_SEED_PROFILES = List.of(
            new LoanSeedProfile("Acme Manufacturing LLC", 5_000_000, LocalDate.of(2024, 1, 15), LoanHealth.STRONG, false),
            new LoanSeedProfile("Northbridge Logistics Group", 8_500_000, LocalDate.of(2023, 11, 1), LoanHealth.WATCH, false),
            new LoanSeedProfile("Horizon Health Clinics", 4_200_000, LocalDate.of(2024, 2, 20), LoanHealth.STRONG, false),
            new LoanSeedProfile("Summit Retail Holdings", 6_000_000, LocalDate.of(2023, 10, 10), LoanHealth.STRESSED, false),
            new LoanSeedProfile("BlueRiver Foods Inc.", 3_750_000, LocalDate.of(2024, 4, 5), LoanHealth.WATCH, false),
            new LoanSeedProfile("MetroBuild Contractors", 9_300_000, LocalDate.of(2023, 9, 12), LoanHealth.STRESSED, true),
            new LoanSeedProfile("Sierra Energy Partners", 12_000_000, LocalDate.of(2023, 8, 30), LoanHealth.WATCH, false),
            new LoanSeedProfile("Apex Pharma Distribution", 7_100_000, LocalDate.of(2024, 1, 5), LoanHealth.STRONG, false),
            new LoanSeedProfile("Granite Auto Components", 4_900_000, LocalDate.of(2024, 3, 18), LoanHealth.WATCH, false),
            new LoanSeedProfile("Evergreen Packaging Co.", 5_400_000, LocalDate.of(2023, 12, 6), LoanHealth.STRONG, false),
            new LoanSeedProfile("UrbanGrid Telecom Services", 11_200_000, LocalDate.of(2023, 7, 14), LoanHealth.STRESSED, false),
            new LoanSeedProfile("Harbor Maritime Supply", 6_800_000, LocalDate.of(2024, 2, 2), LoanHealth.WATCH, false)
    );

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedData(
            AppModeProperties appModeProperties,
            LoanRepository loanRepository,
            UserAccountRepository userAccountRepository,
            CommentRepository commentRepository,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            LoanService loanService,
            CovenantService covenantService,
            FinancialStatementService financialStatementService,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!appModeProperties.sampleContentAvailable()) {
                return;
            }
            if (userAccountRepository.count() == 0) {
                seedUsers(userAccountRepository, passwordEncoder);
            }
            if (loanRepository.count() == 0) {
                seedPortfolio(
                        loanService,
                        covenantService,
                        financialStatementService,
                        commentRepository,
                        covenantResultRepository,
                        alertRepository
                );
            }
        };
    }

    private void seedUsers(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        createUser(userAccountRepository, passwordEncoder, "analyst@demo.com", "Demo123!", "ANALYST");
        createUser(userAccountRepository, passwordEncoder, "risklead@demo.com", "Demo123!", "RISK_LEAD");
        createUser(userAccountRepository, passwordEncoder, "admin@demo.com", "Demo123!", "ADMIN");

        createUser(userAccountRepository, passwordEncoder, "analyst.jordan@demo.com", "Demo123!", "ANALYST");
        createUser(userAccountRepository, passwordEncoder, "analyst.maya@demo.com", "Demo123!", "ANALYST");
        createUser(userAccountRepository, passwordEncoder, "analyst.oliver@demo.com", "Demo123!", "ANALYST");
        createUser(userAccountRepository, passwordEncoder, "risk.chen@demo.com", "Demo123!", "RISK_LEAD");
        createUser(userAccountRepository, passwordEncoder, "risk.ramirez@demo.com", "Demo123!", "RISK_LEAD");
        createUser(userAccountRepository, passwordEncoder, "admin.ops@demo.com", "Demo123!", "ADMIN");
    }

    private void seedPortfolio(
            LoanService loanService,
            CovenantService covenantService,
            FinancialStatementService financialStatementService,
            CommentRepository commentRepository,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository
    ) {
        for (int i = 0; i < LOAN_SEED_PROFILES.size(); i++) {
            LoanSeedProfile profile = LOAN_SEED_PROFILES.get(i);
            Loan loan = loanService.createLoan(new CreateLoanRequest(
                    profile.borrowerName(),
                    bd(profile.principalAmount()),
                    profile.startDate()
            ));

            addStandardCovenants(covenantService, loan.getId(), profile.principalAmount(), profile.health());
            seedFinancialHistory(
                    financialStatementService,
                    covenantResultRepository,
                    alertRepository,
                    loan.getId(),
                    profile.principalAmount(),
                    profile.health(),
                    i
            );
            seedComments(commentRepository, loan, profile, i);

            if (profile.closedAfterSeeding()) {
                loanService.closeLoan(loan.getId());
            }
        }
    }

    private void addStandardCovenants(
            CovenantService covenantService,
            Long loanId,
            double principalAmount,
            LoanHealth health
    ) {
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.CURRENT_RATIO,
                bd(1.20),
                ComparisonType.GREATER_THAN_EQUAL,
                SeverityLevel.HIGH
        ));
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.DEBT_TO_EQUITY,
                bd(2.50),
                ComparisonType.LESS_THAN_EQUAL,
                SeverityLevel.HIGH
        ));
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.DSCR,
                bd(1.25),
                ComparisonType.GREATER_THAN_EQUAL,
                SeverityLevel.HIGH
        ));
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.INTEREST_COVERAGE,
                bd(2.00),
                ComparisonType.GREATER_THAN_EQUAL,
                SeverityLevel.MEDIUM
        ));
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.DEBT_TO_EBITDA,
                bd(4.00),
                ComparisonType.LESS_THAN_EQUAL,
                SeverityLevel.HIGH
        ));
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.QUICK_RATIO,
                bd(1.00),
                ComparisonType.GREATER_THAN_EQUAL,
                SeverityLevel.MEDIUM
        ));
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.FIXED_CHARGE_COVERAGE,
                bd(1.35),
                ComparisonType.GREATER_THAN_EQUAL,
                SeverityLevel.MEDIUM
        ));

        double tnwThreshold = switch (health) {
            case STRONG -> principalAmount * 0.40;
            case WATCH -> principalAmount * 0.34;
            case STRESSED -> principalAmount * 0.30;
        };
        covenantService.createCovenant(loanId, new CreateCovenantRequest(
                CovenantType.TANGIBLE_NET_WORTH,
                bd(tnwThreshold),
                ComparisonType.GREATER_THAN_EQUAL,
                SeverityLevel.MEDIUM
        ));
    }

    private void seedFinancialHistory(
            FinancialStatementService financialStatementService,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            Long loanId,
            double principalAmount,
            LoanHealth health,
            int loanIndex
    ) {
        int year = 2024;
        int quarter = 1;

        for (int qIndex = 0; qIndex < 8; qIndex++) {
            double healthShift = switch (health) {
                case STRONG -> 0.0;
                case WATCH -> 0.22;
                case STRESSED -> 0.48;
            };
            double seasonal = (qIndex % 2 == 0) ? 0.03 : -0.02;

            double currentRatioTarget = clamp(1.75 - (qIndex * 0.06) - healthShift + seasonal, 0.78, 2.10);
            double debtToEquityTarget = clamp(1.70 + (qIndex * 0.10) + healthShift, 1.40, 3.80);
            double dscrTarget = clamp(1.85 - (qIndex * 0.07) - (healthShift * 0.90), 0.72, 2.10);
            double icrTarget = clamp(2.95 - (qIndex * 0.12) - (healthShift * 0.95), 0.82, 3.10);
            double debtToEbitdaTarget = clamp(2.80 + (qIndex * 0.15) + (healthShift * 1.20), 2.30, 5.30);
            double fixedChargeCoverageTarget = clamp(1.65 - (qIndex * 0.06) - (healthShift * 0.55), 0.92, 1.85);
            double quickRatioTarget = clamp(currentRatioTarget - 0.24, 0.62, 1.80);

            double currentLiabilities = principalAmount * (0.14 + (qIndex * 0.009) + (loanIndex * 0.0015));
            double currentAssets = currentLiabilities * currentRatioTarget;
            double equity = principalAmount * (0.56 - (qIndex * 0.015) - (healthShift * 0.10));
            double debt = equity * debtToEquityTarget;
            double interestExpense = principalAmount * (0.012 + (qIndex * 0.0008));
            double ebit = interestExpense * icrTarget;
            double debtService = principalAmount * (0.043 + (qIndex * 0.0012));
            double noi = debtService * dscrTarget;
            double ebitda = debt / debtToEbitdaTarget;
            double fixedCharges = ebitda / fixedChargeCoverageTarget;
            double inventory = currentAssets * clamp(1 - (quickRatioTarget / currentRatioTarget), 0.08, 0.35);

            double totalAssets = principalAmount * (1.70 + (qIndex * 0.030) - (healthShift * 0.05));
            double intangibleAssets = totalAssets * (0.04 + ((loanIndex % 4) * 0.006));
            double totalLiabilities = debt + (currentLiabilities * 0.62);

            OffsetDateTime submissionTs = quarterEndUtc(year, quarter).plusDays(loanIndex % 5).plusHours(10);
            FinancialStatement saved = financialStatementService.submitStatement(loanId, new SubmitFinancialStatementRequest(
                    PeriodType.QUARTERLY,
                    year,
                    quarter,
                    bd(currentAssets),
                    bd(currentLiabilities),
                    bd(debt),
                    bd(equity),
                    bd(ebit),
                    bd(interestExpense),
                    bd(noi),
                    bd(debtService),
                    bd(intangibleAssets),
                    bd(ebitda),
                    bd(fixedCharges),
                    bd(inventory),
                    bd(totalAssets),
                    bd(totalLiabilities),
                    submissionTs
            ));

            alignMonitoringTimestamps(saved, submissionTs, loanIndex, qIndex, covenantResultRepository, alertRepository);

            quarter++;
            if (quarter > 4) {
                quarter = 1;
                year++;
            }
        }
    }

    private void alignMonitoringTimestamps(
            FinancialStatement statement,
            OffsetDateTime baseTimestamp,
            int loanIndex,
            int quarterIndex,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository
    ) {
        List<CovenantResult> results = covenantResultRepository.findByFinancialStatementIdAndSupersededFalse(statement.getId());
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setEvaluationTimestampUtc(baseTimestamp.plusHours(2).plusMinutes(i * 3L));
        }
        covenantResultRepository.saveAll(results);

        List<Alert> alerts = alertRepository.findByFinancialStatementIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(
                statement.getId()
        );
        for (int i = 0; i < alerts.size(); i++) {
            Alert alert = alerts.get(i);
            OffsetDateTime triggeredAt = baseTimestamp.plusHours(3).plusMinutes(i * 5L);
            alert.setTriggeredTimestampUtc(triggeredAt);

            int stateSelector = (loanIndex + quarterIndex + i) % 5;
            if (stateSelector == 0) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setAcknowledgedBy("risklead@demo.com");
                alert.setAcknowledgedAt(triggeredAt.plusHours(5));
                alert.setResolvedBy("risklead@demo.com");
                alert.setResolvedAt(triggeredAt.plusDays(2));
                alert.setResolutionNotes("Resolved after borrower provided variance explanation and revised projections.");
            } else if (stateSelector <= 2) {
                alert.setStatus(AlertStatus.UNDER_REVIEW);
                alert.setAcknowledgedBy("analyst.jordan@demo.com");
                alert.setAcknowledgedAt(triggeredAt.plusHours(3));
                alert.setResolvedBy(null);
                alert.setResolvedAt(null);
                alert.setResolutionNotes("Under review with RM; pending covenant waiver decision.");
            } else if (stateSelector == 3) {
                alert.setStatus(AlertStatus.ACKNOWLEDGED);
                alert.setAcknowledgedBy("analyst.maya@demo.com");
                alert.setAcknowledgedAt(triggeredAt.plusHours(2));
                alert.setResolvedBy(null);
                alert.setResolvedAt(null);
                alert.setResolutionNotes(null);
            }
        }
        alertRepository.saveAll(alerts);
    }

    private void seedComments(
            CommentRepository commentRepository,
            Loan loan,
            LoanSeedProfile profile,
            int loanIndex
    ) {
        List<String> templates = List.of(
                "Quarterly package reviewed. Borrower highlighted margin pressure in core segment.",
                "Follow-up requested on covenant trend and inventory build-up assumptions.",
                "Risk call completed; action plan captured with relationship manager.",
                "Pending revised forecast submission before next credit committee."
        );
        List<String> authors = List.of(
                "analyst@demo.com",
                "analyst.jordan@demo.com",
                "analyst.maya@demo.com",
                "risklead@demo.com"
        );

        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusDays(40L - (loanIndex * 2L));
        for (int i = 0; i < templates.size(); i++) {
            Comment comment = new Comment();
            comment.setLoan(loan);
            comment.setCommentText(templates.get(i));
            comment.setCreatedBy(authors.get((loanIndex + i) % authors.size()));
            comment.setCreatedAt(base.plusDays(i * 6L).plusHours(loanIndex % 5));
            commentRepository.save(comment);
        }

        if (profile.closedAfterSeeding()) {
            Comment closure = new Comment();
            closure.setLoan(loan);
            closure.setCommentText("Loan moved to CLOSED after final remediation and credit exit review.");
            closure.setCreatedBy("admin.ops@demo.com");
            closure.setCreatedAt(base.plusDays(30));
            commentRepository.save(closure);
        }
    }

    private void createUser(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String password,
            String rolesCsv
    ) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRolesCsv(rolesCsv);
        user.setActive(true);
        userAccountRepository.save(user);
    }

    private static OffsetDateTime quarterEndUtc(int year, int quarter) {
        return switch (quarter) {
            case 1 -> OffsetDateTime.of(year, 3, 31, 12, 0, 0, 0, ZoneOffset.UTC);
            case 2 -> OffsetDateTime.of(year, 6, 30, 12, 0, 0, 0, ZoneOffset.UTC);
            case 3 -> OffsetDateTime.of(year, 9, 30, 12, 0, 0, 0, ZoneOffset.UTC);
            default -> OffsetDateTime.of(year, 12, 31, 12, 0, 0, 0, ZoneOffset.UTC);
        };
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum LoanHealth {
        STRONG,
        WATCH,
        STRESSED
    }

    private record LoanSeedProfile(
            String borrowerName,
            double principalAmount,
            LocalDate startDate,
            LoanHealth health,
            boolean closedAfterSeeding
    ) {
    }
}


