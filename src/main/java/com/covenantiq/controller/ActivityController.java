package com.covenantiq.controller;

import com.covenantiq.dto.response.ActivityLogResponse;
import com.covenantiq.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class ActivityController {

    private final ActivityLogService activityLogService;

    public ActivityController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/loans/{loanId}/activity")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public Page<ActivityLogResponse> loanActivity(@PathVariable Long loanId, Pageable pageable) {
        return activityLogService.getActivityForLoan(loanId, pageable);
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('RISK_LEAD','ADMIN')")
    public Page<ActivityLogResponse> activity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Pageable pageable
    ) {
        return activityLogService.getActivityForDateRange(start, end, pageable);
    }
}
