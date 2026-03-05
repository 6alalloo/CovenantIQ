package com.covenantiq.dto.response;

import com.covenantiq.enums.ChangeRequestStatus;
import com.covenantiq.enums.ChangeRequestType;

import java.time.OffsetDateTime;
import java.util.List;

public record ChangeRequestResponse(
        Long id,
        ChangeRequestType type,
        ChangeRequestStatus status,
        String requestedBy,
        OffsetDateTime requestedAt,
        String approvedBy,
        OffsetDateTime approvedAt,
        String justification,
        List<ChangeRequestItemResponse> items
) {
}
