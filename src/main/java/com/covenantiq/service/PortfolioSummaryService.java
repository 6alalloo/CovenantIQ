package com.covenantiq.service;

import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.domain.Loan;
import com.covenantiq.domain.CovenantResult;
import com.covenantiq.dto.response.PortfolioSummaryResponse;
import com.covenantiq.dto.response.PortfolioTrendPointResponse;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.CovenantResultStatus;
import com.covenantiq.enums.LoanStatus;
import com.covenantiq.repository.AlertRepository;
import com.covenantiq.repository.CovenantResultRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import com.covenantiq.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioSummaryService {

    private final LoanRepository loanRepository;
    private final FinancialStatementService financialStatementService;
    private final CovenantResultRepository covenantResultRepository;
    private final AlertRepository alertRepository;
    private final FinancialStatementRepository financialStatementRepository;

    public PortfolioSummaryService(
            LoanRepository loanRepository,
            FinancialStatementService financialStatementService,
            CovenantResultRepository covenantResultRepository,
            AlertRepository alertRepository,
            FinancialStatementRepository financialStatementRepository
    ) {
        this.loanRepository = loanRepository;
        this.financialStatementService = financialStatementService;
        this.covenantResultRepository = covenantResultRepository;
        this.alertRepository = alertRepository;
        this.financialStatementRepository = financialStatementRepository;
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary() {
        List<Loan> activeLoans = getActiveLoans();

        long totalBreaches = 0;
        long highRisk = 0;
        long mediumRisk = 0;
        long lowRisk = 0;

        for (Loan loan : activeLoans) {
            FinancialStatement latest = financialStatementService.getLatestStatement(loan.getId());
            if (latest == null) {
                lowRisk++;
                continue;
            }
            long breachCount = getBreachCount(latest.getId());
            totalBreaches += breachCount;
            RiskCounts riskCounts = classifyRisk(breachCount);
            highRisk += riskCounts.highRiskLoanCount();
            mediumRisk += riskCounts.mediumRiskLoanCount();
            lowRisk += riskCounts.lowRiskLoanCount();
        }

        long totalOpen = alertRepository.countBySupersededFalseAndStatusIn(EnumSet.of(AlertStatus.OPEN));
        long totalUnderReview = alertRepository.countBySupersededFalseAndStatusIn(EnumSet.of(AlertStatus.UNDER_REVIEW));

        return new PortfolioSummaryResponse(
                activeLoans.size(),
                totalBreaches,
                highRisk,
                mediumRisk,
                lowRisk,
                totalOpen,
                totalUnderReview
        );
    }

    @Transactional(readOnly = true)
    public List<PortfolioTrendPointResponse> getTrend() {
        List<Loan> activeLoans = getActiveLoans();
        if (activeLoans.isEmpty()) {
            return List.of();
        }

        List<Long> loanIds = activeLoans.stream().map(Loan::getId).toList();
        List<FinancialStatement> statements = financialStatementRepository
                .findByLoanIdInAndSupersededFalseOrderBySubmissionTimestampUtcAscIdAsc(loanIds);
        if (statements.isEmpty()) {
            return List.of();
        }

        List<Long> statementIds = statements.stream().map(FinancialStatement::getId).toList();
        Map<Long, Long> breachCountByStatementId = covenantResultRepository
                .findByFinancialStatementIdInAndSupersededFalse(statementIds)
                .stream()
                .filter(result -> result.getStatus() == CovenantResultStatus.BREACH)
                .collect(java.util.stream.Collectors.groupingBy(
                        result -> result.getFinancialStatement().getId(),
                        java.util.stream.Collectors.counting()
                ));

        Map<QuarterKey, List<FinancialStatement>> statementsByQuarter = new HashMap<>();
        for (FinancialStatement statement : statements) {
            statementsByQuarter
                    .computeIfAbsent(QuarterKey.from(statement.getSubmissionTimestampUtc()), ignored -> new ArrayList<>())
                    .add(statement);
        }

        List<QuarterKey> quarters = statementsByQuarter.keySet().stream()
                .sorted()
                .toList();

        Map<Long, FinancialStatement> latestStatementByLoanId = new HashMap<>();
        List<PortfolioTrendPointResponse> trend = new ArrayList<>();
        for (QuarterKey quarter : quarters) {
            for (FinancialStatement statement : statementsByQuarter.get(quarter)) {
                latestStatementByLoanId.merge(
                        statement.getLoan().getId(),
                        statement,
                        (existing, candidate) -> existing.getSubmissionTimestampUtc().isAfter(candidate.getSubmissionTimestampUtc()) ? existing : candidate
                );
            }

            long highRisk = 0;
            long mediumRisk = 0;
            long lowRisk = 0;

            for (Loan loan : activeLoans) {
                FinancialStatement latestStatement = latestStatementByLoanId.get(loan.getId());
                if (latestStatement == null) {
                    continue;
                }
                long breachCount = breachCountByStatementId.getOrDefault(latestStatement.getId(), 0L);
                RiskCounts riskCounts = classifyRisk(breachCount);
                highRisk += riskCounts.highRiskLoanCount();
                mediumRisk += riskCounts.mediumRiskLoanCount();
                lowRisk += riskCounts.lowRiskLoanCount();
            }

            trend.add(new PortfolioTrendPointResponse(quarter.label(), highRisk, mediumRisk, lowRisk));
        }

        return trend;
    }

    private List<Loan> getActiveLoans() {
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
                .toList();
    }

    private long getBreachCount(Long statementId) {
        return covenantResultRepository.countByFinancialStatementIdAndStatusAndSupersededFalse(
                statementId, CovenantResultStatus.BREACH
        );
    }

    private RiskCounts classifyRisk(long breachCount) {
        if (breachCount >= 2) {
            return new RiskCounts(1, 0, 0);
        }
        if (breachCount == 1) {
            return new RiskCounts(0, 1, 0);
        }
        return new RiskCounts(0, 0, 1);
    }

    private record RiskCounts(long highRiskLoanCount, long mediumRiskLoanCount, long lowRiskLoanCount) {
    }

    private record QuarterKey(int year, int quarter) implements Comparable<QuarterKey> {
        private static QuarterKey from(OffsetDateTime timestamp) {
            return new QuarterKey(timestamp.getYear(), ((timestamp.getMonthValue() - 1) / 3) + 1);
        }

        private String label() {
            return year + " Q" + quarter;
        }

        @Override
        public int compareTo(QuarterKey other) {
            return Comparator.comparingInt(QuarterKey::year)
                    .thenComparingInt(QuarterKey::quarter)
                    .compare(this, other);
        }
    }
}
