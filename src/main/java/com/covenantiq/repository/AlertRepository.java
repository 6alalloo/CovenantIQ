package com.covenantiq.repository;

import com.covenantiq.domain.Alert;
import com.covenantiq.enums.AlertStatus;
import com.covenantiq.enums.AlertType;
import com.covenantiq.enums.SeverityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByLoanIdAndSupersededFalse(Long loanId, Pageable pageable);

    List<Alert> findByFinancialStatementIdAndSupersededFalse(Long financialStatementId);

    List<Alert> findByFinancialStatementIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(Long financialStatementId);

    long countByFinancialStatementIdAndAlertTypeAndSupersededFalse(Long financialStatementId, AlertType alertType);

    long countByFinancialStatementIdAndAlertTypeAndStatusNotAndSupersededFalse(
            Long financialStatementId, AlertType alertType, AlertStatus status
    );

    boolean existsByFinancialStatementIdAndAlertTypeAndSeverityLevelAndSupersededFalse(
            Long financialStatementId, AlertType alertType, SeverityLevel severityLevel
    );

    long countBySupersededFalseAndStatusIn(Set<AlertStatus> statuses);

    List<Alert> findByLoanIdAndSupersededFalseOrderByTriggeredTimestampUtcDescIdDesc(Long loanId);
}
