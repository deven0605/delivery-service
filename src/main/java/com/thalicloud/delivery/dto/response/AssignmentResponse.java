package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AssignmentResponse {
    private final UUID id;
    private final String orderId;
    private final String kitchenName;
    private final double kitchenDistanceKm;
    private final String dropLocality;
    private final long estimatedPayoutPaise;
    private final double estimatedDistanceKm;
    private final int itemCount;
    private final AssignmentStatus status;
    private final LocalDateTime offeredAt;
    private final LocalDateTime expiresAt;

    public static AssignmentResponse from(DeliveryAssignment a) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .orderId(a.getOrderId())
                .kitchenName(a.getKitchenName())
                .kitchenDistanceKm(a.getKitchenDistanceKm())
                .dropLocality(a.getDropLocality())
                .estimatedPayoutPaise(a.getEstimatedPayoutPaise())
                .estimatedDistanceKm(a.getEstimatedDistanceKm())
                .itemCount(a.getItemCount())
                .status(a.getStatus())
                .offeredAt(a.getOfferedAt())
                .expiresAt(a.getExpiresAt())
                .build();
    }
}
