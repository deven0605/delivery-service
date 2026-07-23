package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.Payout;
import com.thalicloud.delivery.enums.PayoutStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// M8.2 — Payout History (FR-8.4).
@Getter
@Builder
public class PayoutResponse {
    private final UUID id;
    private final long amountPaise;
    private final String destinationReference;
    private final PayoutStatus status;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDateTime initiatedAt;
    private final LocalDateTime paidAt;

    public static PayoutResponse from(Payout p) {
        return PayoutResponse.builder()
                .id(p.getId())
                .amountPaise(p.getAmountPaise())
                .destinationReference(p.getDestinationReference())
                .status(p.getStatus())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .initiatedAt(p.getInitiatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }
}
