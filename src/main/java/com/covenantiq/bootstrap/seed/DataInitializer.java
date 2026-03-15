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

    private static final SeedScenario TEST_SEED_SCENARIO = new SeedScenario(
            List.of(
                    new LoanSeedProfile("Acme Manufacturing LLC", 5_000_000, LocalDate.of(2024, 1, 15), LoanHealth.STRONG, false, null, null, "Manufacturing", "Core U.S. industrial borrower"),
                    new LoanSeedProfile("Northbridge Logistics Group", 8_500_000, LocalDate.of(2023, 11, 1), LoanHealth.WATCH, false, null, null, "Transportation", "Regional freight and warehousing group"),
                    new LoanSeedProfile("Horizon Health Clinics", 4_200_000, LocalDate.of(2024, 2, 20), LoanHealth.STRONG, false, "AGENT_PORTAL", "HLTH-2042", "Healthcare", "Multi-site outpatient provider"),
                    new LoanSeedProfile("Summit Retail Holdings", 6_000_000, LocalDate.of(2023, 10, 10), LoanHealth.STRESSED, false, null, null, "Retail", "Specialty retail platform under turnaround"),
                    new LoanSeedProfile("BlueRiver Foods Inc.", 3_750_000, LocalDate.of(2024, 4, 5), LoanHealth.WATCH, false, "CORE_BANKING", "FOOD-1875", "Food & Beverage", "Branded packaged foods borrower"),
                    new LoanSeedProfile("MetroBuild Contractors", 9_300_000, LocalDate.of(2023, 9, 12), LoanHealth.STRESSED, true, null, null, "Construction", "Project-based contractor with volatile backlog"),
                    new LoanSeedProfile("Sierra Energy Partners", 12_000_000, LocalDate.of(2023, 8, 30), LoanHealth.WATCH, false, "TREASURY_SERVICES", "ENRG-8801", "Energy", "Mid-market services business"),
                    new LoanSeedProfile("Apex Pharma Distribution", 7_100_000, LocalDate.of(2024, 1, 5), LoanHealth.STRONG, false, "AGENT_PORTAL", "PHRM-7110", "Healthcare", "Drug distribution and specialty logistics"),
                    new LoanSeedProfile("Granite Auto Components", 4_900_000, LocalDate.of(2024, 3, 18), LoanHealth.WATCH, false, null, null, "Automotive", "Tier-two supplier in light vehicle platform"),
                    new LoanSeedProfile("Evergreen Packaging Co.", 5_400_000, LocalDate.of(2023, 12, 6), LoanHealth.STRONG, false, "CORE_BANKING", "PKG-5400", "Packaging", "Consumer packaging supplier"),
                    new LoanSeedProfile("UrbanGrid Telecom Services", 11_200_000, LocalDate.of(2023, 7, 14), LoanHealth.STRESSED, false, "TREASURY_SERVICES", "TEL-1120", "Telecom", "Fiber deployment and field services"),
                    new LoanSeedProfile("Harbor Maritime Supply", 6_800_000, LocalDate.of(2024, 2, 2), LoanHealth.WATCH, false, null, null, "Marine", "Marine equipment distributor")
            ),
            2024,
            2024,
            2025,
            4
    );

    private static final SeedScenario DEMO_SEED_SCENARIO = new SeedScenario(
            List.of(
                    new LoanSeedProfile("Acme Manufacturing LLC", 5_000_000, LocalDate.of(2022, 1, 15), LoanHealth.STRONG, false, null, null, "Manufacturing", "Precision components platform for diversified OEMs"),
                    new LoanSeedProfile("Northbridge Logistics Group", 8_500_000, LocalDate.of(2021, 11, 1), LoanHealth.WATCH, false, "CORE_BANKING", "LOG-8501", "Transportation", "Regional drayage, warehousing, and cold-chain operator"),
                    new LoanSeedProfile("Horizon Health Clinics", 4_200_000, LocalDate.of(2022, 2, 20), LoanHealth.STRONG, false, "AGENT_PORTAL", "HLTH-2042", "Healthcare", "Multi-site outpatient provider with sponsor support"),
                    new LoanSeedProfile("Summit Retail Holdings", 6_000_000, LocalDate.of(2021, 10, 10), LoanHealth.STRESSED, false, null, null, "Retail", "Specialty retail platform under turnaround plan"),
                    new LoanSeedProfile("BlueRiver Foods Inc.", 3_750_000, LocalDate.of(2023, 4, 5), LoanHealth.WATCH, false, "CORE_BANKING", "FOOD-1875", "Food & Beverage", "Branded packaged foods borrower"),
                    new LoanSeedProfile("MetroBuild Contractors", 9_300_000, LocalDate.of(2021, 9, 12), LoanHealth.STRESSED, true, null, null, "Construction", "Project-based contractor with volatile backlog"),
                    new LoanSeedProfile("Sierra Energy Partners", 12_000_000, LocalDate.of(2022, 8, 30), LoanHealth.WATCH, false, "TREASURY_SERVICES", "ENRG-8801", "Energy", "Field services business exposed to commodity swings"),
                    new LoanSeedProfile("Apex Pharma Distribution", 7_100_000, LocalDate.of(2023, 1, 5), LoanHealth.STRONG, false, "AGENT_PORTAL", "PHRM-7110", "Healthcare", "Drug distribution and specialty logistics"),
                    new LoanSeedProfile("Granite Auto Components", 4_900_000, LocalDate.of(2022, 3, 18), LoanHealth.WATCH, false, null, null, "Automotive", "Tier-two supplier in light vehicle platform"),
                    new LoanSeedProfile("Evergreen Packaging Co.", 5_400_000, LocalDate.of(2021, 12, 6), LoanHealth.STRONG, false, "CORE_BANKING", "PKG-5400", "Packaging", "Consumer packaging supplier"),
                    new LoanSeedProfile("UrbanGrid Telecom Services", 11_200_000, LocalDate.of(2021, 7, 14), LoanHealth.STRESSED, false, "TREASURY_SERVICES", "TEL-1120", "Telecom", "Fiber deployment and field services borrower"),
                    new LoanSeedProfile("Harbor Maritime Supply", 6_800_000, LocalDate.of(2023, 2, 2), LoanHealth.WATCH, false, null, null, "Marine", "Marine equipment and parts distributor"),
                    new LoanSeedProfile("Crestline Hospitality Group", 8_900_000, LocalDate.of(2021, 6, 17), LoanHealth.WATCH, false, "AGENT_PORTAL", "HOSP-8900", "Hospitality", "Select-service hotel portfolio across the Southeast"),
                    new LoanSeedProfile("IronPeak Metals Fabrication", 5_600_000, LocalDate.of(2022, 5, 9), LoanHealth.STRONG, false, null, null, "Industrial", "Fabrication and machining operator"),
                    new LoanSeedProfile("Willow Creek Senior Living", 7_800_000, LocalDate.of(2021, 4, 21), LoanHealth.WATCH, false, "CORE_BANKING", "SNR-7800", "Healthcare", "Senior care operator with occupancy pressure"),
                    new LoanSeedProfile("Atlas Cold Storage Holdings", 10_400_000, LocalDate.of(2022, 9, 1), LoanHealth.STRONG, false, "TREASURY_SERVICES", "COLD-1040", "Logistics", "Temperature-controlled warehouse network"),
                    new LoanSeedProfile("Pioneer Office Interiors", 3_200_000, LocalDate.of(2023, 7, 25), LoanHealth.STRESSED, false, null, null, "Business Services", "Commercial furnishings and fit-out provider"),
                    new LoanSeedProfile("Redstone Building Products", 6_200_000, LocalDate.of(2021, 3, 14), LoanHealth.WATCH, false, "AGENT_PORTAL", "BLD-6200", "Building Products", "Residential and light-commercial products manufacturer"),
                    new LoanSeedProfile("Nova Agritech Solutions", 4_600_000, LocalDate.of(2022, 11, 8), LoanHealth.STRONG, false, "CORE_BANKING", "AGR-4600", "Agriculture", "Crop-input and precision ag distributor"),
                    new LoanSeedProfile("Beacon Environmental Services", 5_100_000, LocalDate.of(2021, 8, 19), LoanHealth.WATCH, false, null, null, "Environmental", "Compliance and remediation services platform"),
                    new LoanSeedProfile("Oakstone Furniture Manufacturing", 6_900_000, LocalDate.of(2022, 1, 28), LoanHealth.STRESSED, false, "TREASURY_SERVICES", "FURN-6900", "Consumer", "Case goods manufacturer with margin compression"),
                    new LoanSeedProfile("Helios Solar Integrators", 9_700_000, LocalDate.of(2023, 5, 30), LoanHealth.STRONG, false, "AGENT_PORTAL", "SOL-9700", "Energy", "Commercial solar EPC and maintenance provider"),
                    new LoanSeedProfile("Ridgeway Specialty Chemicals", 7_300_000, LocalDate.of(2021, 9, 7), LoanHealth.WATCH, false, "CORE_BANKING", "CHEM-7300", "Chemicals", "Niche industrial chemical blender"),
                    new LoanSeedProfile("Delta Learning Systems", 3_900_000, LocalDate.of(2022, 6, 11), LoanHealth.STRONG, false, null, null, "Education", "Career and compliance training platform"),
                    new LoanSeedProfile("Sterling Security Integrators", 4_800_000, LocalDate.of(2023, 2, 13), LoanHealth.WATCH, false, "AGENT_PORTAL", "SEC-4800", "Technology", "Low-voltage and building security integrator"),
                    new LoanSeedProfile("Canyon Waste Recovery", 8_100_000, LocalDate.of(2021, 1, 22), LoanHealth.STRONG, false, "TREASURY_SERVICES", "WASTE-8100", "Environmental", "Regional recycling and hauling company"),
                    new LoanSeedProfile("BlueOak Pet Nutrition", 4_400_000, LocalDate.of(2022, 10, 4), LoanHealth.WATCH, false, "CORE_BANKING", "PET-4400", "Consumer", "Premium pet food manufacturer"),
                    new LoanSeedProfile("Mercury Aviation Services", 11_600_000, LocalDate.of(2021, 5, 16), LoanHealth.STRESSED, false, null, null, "Aviation", "Ground handling and MRO services platform"),
                    new LoanSeedProfile("Keystone Insurance Administrators", 5_700_000, LocalDate.of(2023, 3, 6), LoanHealth.STRONG, false, "AGENT_PORTAL", "INS-5700", "Financial Services", "Third-party insurance administrator"),
                    new LoanSeedProfile("Vista Diagnostic Imaging", 6_100_000, LocalDate.of(2022, 7, 29), LoanHealth.WATCH, false, "CORE_BANKING", "IMG-6100", "Healthcare", "Diagnostic imaging operator"),
                    new LoanSeedProfile("Summerset Landscape Partners", 3_500_000, LocalDate.of(2024, 2, 12), LoanHealth.STRONG, false, null, null, "Services", "Commercial landscaping and snow removal provider"),
                    new LoanSeedProfile("Frontier Data Centers", 12_400_000, LocalDate.of(2023, 9, 20), LoanHealth.STRONG, false, "TREASURY_SERVICES", "DC-12400", "Technology", "Edge data center infrastructure borrower"),
                    new LoanSeedProfile("Mariner Home Goods Wholesale", 4_100_000, LocalDate.of(2022, 4, 26), LoanHealth.STRESSED, false, "CORE_BANKING", "HOME-4100", "Wholesale", "Import-heavy home goods distributor"),
                    new LoanSeedProfile("Tidal Water Utilities Services", 9_100_000, LocalDate.of(2021, 2, 18), LoanHealth.WATCH, false, "AGENT_PORTAL", "UTIL-9100", "Utilities", "Water infrastructure operations contractor"),
                    new LoanSeedProfile("Prairie Fresh Produce Packers", 5_200_000, LocalDate.of(2023, 6, 14), LoanHealth.WATCH, false, null, null, "Food & Beverage", "Produce packing and distribution borrower"),
                    new LoanSeedProfile("NorthStar Aviation Interiors", 4_700_000, LocalDate.of(2024, 1, 9), LoanHealth.STRONG, false, "AGENT_PORTAL", "AIR-4700", "Aviation", "Aircraft interiors and refurbishment supplier")
            ),
            2021,
            2024,
            2026,
            1
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
                SeedScenario seedScenario = appModeProperties.isDemoEnabled() ? DEMO_SEED_SCENARIO : TEST_SEED_SCENARIO;
                seedPortfolio(
                        loanRepository,
                        loanService,
                        covenantService,
                        financialStatementService,
                        commentRepository,
                        covenantResultRepository,
                        alertRepository,
                        seedScenario
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
            LoanRepository loanRepository,
            LoanService loanService,
            CovenantService covenantService,
            FinancialStatementService financialStatementService,
            CommentRepository commentRepository,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            SeedScenario seedScenario
    ) {
        for (int i = 0; i < seedScenario.profiles().size(); i++) {
            LoanSeedProfile profile = seedScenario.profiles().get(i);
            Loan loan = loanService.createLoan(new CreateLoanRequest(
                    profile.borrowerName(),
                    bd(profile.principalAmount()),
                    profile.startDate()
            ));
            loan = applyLoanMetadata(loanRepository, loan, profile, i);

            addStandardCovenants(covenantService, loan.getId(), profile.principalAmount(), profile.health());
            seedFinancialHistory(
                    financialStatementService,
                    covenantResultRepository,
                    alertRepository,
                    loan.getId(),
                    profile.principalAmount(),
                    profile.health(),
                    profile.startDate(),
                    i,
                    seedScenario
            );
            seedComments(commentRepository, loan, profile, i);

            if (profile.closedAfterSeeding()) {
                loanService.closeLoan(loan.getId());
            }
        }
    }

    private Loan applyLoanMetadata(
            LoanRepository loanRepository,
            Loan loan,
            LoanSeedProfile profile,
            int loanIndex
    ) {
        if (profile.sourceSystem() == null) {
            return loan;
        }

        loan.setSourceSystem(profile.sourceSystem());
        loan.setExternalLoanId(profile.externalLoanId());
        loan.setSyncManaged(true);
        OffsetDateTime sourceUpdatedAt = OffsetDateTime.of(2026, 3, 5 + (loanIndex % 9), 9 + (loanIndex % 5), 15, 0, 0, ZoneOffset.UTC);
        loan.setSourceUpdatedAt(sourceUpdatedAt);
        loan.setLastSyncedAt(sourceUpdatedAt.plusHours(4));
        return loanRepository.save(loan);
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
            LocalDate loanStartDate,
            int loanIndex,
            SeedScenario seedScenario
    ) {
        int historyIndex = 0;
        int firstYear = Math.max(seedScenario.historyStartYear(), loanStartDate.getYear());

        for (int year = firstYear; year <= seedScenario.historyEndYear(); year++) {
            if (year < seedScenario.quarterlyHistoryStartYear()) {
                submitSyntheticStatement(
                        financialStatementService,
                        covenantResultRepository,
                        alertRepository,
                        loanId,
                        principalAmount,
                        health,
                        loanIndex,
                        historyIndex++,
                        PeriodType.ANNUAL,
                        year,
                        null
                );
                continue;
            }

            int firstQuarter = year == loanStartDate.getYear() ? quarterOf(loanStartDate) : 1;
            int lastQuarter = year == seedScenario.historyEndYear() ? seedScenario.finalQuarterForEndYear() : 4;
            for (int quarter = firstQuarter; quarter <= lastQuarter; quarter++) {
                submitSyntheticStatement(
                        financialStatementService,
                        covenantResultRepository,
                        alertRepository,
                        loanId,
                        principalAmount,
                        health,
                        loanIndex,
                        historyIndex++,
                        PeriodType.QUARTERLY,
                        year,
                        quarter
                );
            }
        }
    }

    private void submitSyntheticStatement(
            FinancialStatementService financialStatementService,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            Long loanId,
            double principalAmount,
            LoanHealth health,
            int loanIndex,
            int historyIndex,
            PeriodType periodType,
            int fiscalYear,
            Integer fiscalQuarter
    ) {
            double healthShift = switch (health) {
                case STRONG -> 0.0;
                case WATCH -> 0.22;
                case STRESSED -> 0.48;
            };
            double seasonal = periodType == PeriodType.ANNUAL ? 0.01 : ((historyIndex % 2 == 0) ? 0.03 : -0.02);

            double currentRatioTarget = clamp(1.82 - (historyIndex * 0.05) - healthShift + seasonal, 0.78, 2.20);
            double debtToEquityTarget = clamp(1.65 + (historyIndex * 0.09) + healthShift, 1.35, 3.90);
            double dscrTarget = clamp(1.92 - (historyIndex * 0.06) - (healthShift * 0.90), 0.72, 2.20);
            double icrTarget = clamp(3.05 - (historyIndex * 0.10) - (healthShift * 0.95), 0.82, 3.20);
            double debtToEbitdaTarget = clamp(2.70 + (historyIndex * 0.14) + (healthShift * 1.20), 2.20, 5.50);
            double fixedChargeCoverageTarget = clamp(1.72 - (historyIndex * 0.05) - (healthShift * 0.55), 0.92, 1.95);
            double quickRatioTarget = clamp(currentRatioTarget - 0.24, 0.62, 1.80);

            double currentLiabilities = principalAmount * (0.14 + (historyIndex * 0.008) + (loanIndex * 0.0015));
            double currentAssets = currentLiabilities * currentRatioTarget;
            double equity = principalAmount * (0.56 - (historyIndex * 0.013) - (healthShift * 0.10));
            double debt = equity * debtToEquityTarget;
            double interestExpense = principalAmount * (0.012 + (historyIndex * 0.0007));
            double ebit = interestExpense * icrTarget;
            double debtService = principalAmount * (0.043 + (historyIndex * 0.0010));
            double noi = debtService * dscrTarget;
            double ebitda = debt / debtToEbitdaTarget;
            double fixedCharges = ebitda / fixedChargeCoverageTarget;
            double inventory = currentAssets * clamp(1 - (quickRatioTarget / currentRatioTarget), 0.08, 0.35);

            double totalAssets = principalAmount * (1.70 + (historyIndex * 0.028) - (healthShift * 0.05));
            double intangibleAssets = totalAssets * (0.04 + ((loanIndex % 4) * 0.006));
            double totalLiabilities = debt + (currentLiabilities * 0.62);

            OffsetDateTime submissionTs = periodEndUtc(periodType, fiscalYear, fiscalQuarter).plusDays(loanIndex % 5).plusHours(10);
            FinancialStatement saved = financialStatementService.submitStatement(loanId, new SubmitFinancialStatementRequest(
                    periodType,
                    fiscalYear,
                    fiscalQuarter,
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

            alignMonitoringTimestamps(saved, submissionTs, loanIndex, historyIndex, covenantResultRepository, alertRepository);
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
                profile.sector() + " annual review completed; management emphasized working-capital discipline and pricing execution.",
                "Follow-up requested on " + profile.sector().toLowerCase() + " demand outlook and updated downside case assumptions.",
                "Risk call completed for " + profile.relationshipSummary().toLowerCase() + ". Monitoring cadence remains monthly.",
                profile.sourceSystem() == null
                        ? "Manual borrower package uploaded and covenant notes refreshed ahead of committee."
                        : "External feed from " + profile.sourceSystem() + " reconciled successfully with covenant monitoring records.",
                "Relationship manager requested refreshed forecast package before next portfolio review."
        );
        List<String> authors = List.of(
                "analyst@demo.com",
                "analyst.jordan@demo.com",
                "analyst.maya@demo.com",
                "risklead@demo.com"
        );

        OffsetDateTime base = OffsetDateTime.of(2025, 4, 15, 14, 0, 0, 0, ZoneOffset.UTC).minusDays(loanIndex * 5L);
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

    private static OffsetDateTime periodEndUtc(PeriodType periodType, int year, Integer quarter) {
        if (periodType == PeriodType.ANNUAL) {
            return OffsetDateTime.of(year, 12, 31, 12, 0, 0, 0, ZoneOffset.UTC);
        }
        return quarterEndUtc(year, quarter == null ? 4 : quarter);
    }

    private static OffsetDateTime quarterEndUtc(int year, int quarter) {
        return switch (quarter) {
            case 1 -> OffsetDateTime.of(year, 3, 31, 12, 0, 0, 0, ZoneOffset.UTC);
            case 2 -> OffsetDateTime.of(year, 6, 30, 12, 0, 0, 0, ZoneOffset.UTC);
            case 3 -> OffsetDateTime.of(year, 9, 30, 12, 0, 0, 0, ZoneOffset.UTC);
            default -> OffsetDateTime.of(year, 12, 31, 12, 0, 0, 0, ZoneOffset.UTC);
        };
    }

    private static int quarterOf(LocalDate date) {
        return ((date.getMonthValue() - 1) / 3) + 1;
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
            boolean closedAfterSeeding,
            String sourceSystem,
            String externalLoanId,
            String sector,
            String relationshipSummary
    ) {
    }

    private record SeedScenario(
            List<LoanSeedProfile> profiles,
            int historyStartYear,
            int quarterlyHistoryStartYear,
            int historyEndYear,
            int finalQuarterForEndYear
    ) {
    }
}


