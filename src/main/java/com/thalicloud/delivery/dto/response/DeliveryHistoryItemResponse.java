package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// M9 — Delivery History list row (FR-9.1): date, kitchen, drop area, payout, status.
@Getter
@Builder
public class DeliveryHistoryItemResponse {
    private final UUID assignmentId;
    private final String orderId;
    private final String kitchenName;
    private final String dropLocality;
    private final long payoutPaise;
    private final AssignmentStatus status;
    private final LocalDateTime completedAt;

    public static DeliveryHistoryItemResponse from(DeliveryAssignment a) {
        boolean delivered = a.getStatus() == AssignmentStatus.DELIVERED;
        return DeliveryHistoryItemResponse.builder()
                .assignmentId(a.getId())
                .orderId(a.getOrderId())
                .kitchenName(a.getKitchenName())
                .dropLocality(a.getDropLocality())
                .payoutPaise(delivered ? a.getEstimatedPayoutPaise() : 0)
                .status(a.getStatus())
                .completedAt(delivered ? a.getDeliveredAt() : a.getRespondedAt())
                .build();
    }
}
