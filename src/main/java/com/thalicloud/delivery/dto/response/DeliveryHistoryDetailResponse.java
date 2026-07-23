package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.AssignmentStatus;
import com.thalicloud.delivery.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// M9 — Delivery History detail (FR-9.2): route summary, full timestamp trail,
// payout breakdown.
@Getter
@Builder
public class DeliveryHistoryDetailResponse {
    private final UUID assignmentId;
    private final String orderId;
    private final AssignmentStatus status;

    // ── Route summary ────────────────────────────────────────────────────────
    private final String kitchenName;
    private final double kitchenLatitude;
    private final double kitchenLongitude;
    private final String dropLocality;
    private final double dropLatitude;
    private final double dropLongitude;
    private final double estimatedDistanceKm;
    private final int itemCount;

    // ── Timestamps ───────────────────────────────────────────────────────────
    private final LocalDateTime offeredAt;
    private final LocalDateTime acceptedAt;
    private final LocalDateTime arrivedAtKitchenAt;
    private final LocalDateTime pickedUpAt;
    private final LocalDateTime outForDeliveryAt;
    private final LocalDateTime arrivedAtDropAt;
    private final LocalDateTime deliveredAt;
    private final LocalDateTime cancelledAt;
    private final String cancellationReason;

    // ── Payout breakdown ─────────────────────────────────────────────────────
    private final long baseFarePaise;
    private final long distanceFarePaise;
    private final long incentivePaise;
    private final long totalPayoutPaise;
    private final PaymentMethod paymentMethod;
    private final Long codAmountPaise;

    public static DeliveryHistoryDetailResponse from(DeliveryAssignment a) {
        boolean cancelled = a.getStatus() == AssignmentStatus.CANCELLED_BY_PARTNER
                || a.getStatus() == AssignmentStatus.CANCELLED_BY_CUSTOMER
                || a.getStatus() == AssignmentStatus.CANCELLED_BY_KITCHEN;
        boolean delivered = a.getStatus() == AssignmentStatus.DELIVERED;

        return DeliveryHistoryDetailResponse.builder()
                .assignmentId(a.getId())
                .orderId(a.getOrderId())
                .status(a.getStatus())
                .kitchenName(a.getKitchenName())
                .kitchenLatitude(a.getKitchenLatitude())
                .kitchenLongitude(a.getKitchenLongitude())
                .dropLocality(a.getDropLocality())
                .dropLatitude(a.getDropLatitude())
                .dropLongitude(a.getDropLongitude())
                .estimatedDistanceKm(a.getEstimatedDistanceKm())
                .itemCount(a.getItemCount())
                .offeredAt(a.getOfferedAt())
                .acceptedAt(a.getAcceptedAt())
                .arrivedAtKitchenAt(a.getArrivedAtKitchenAt())
                .pickedUpAt(a.getPickedUpAt())
                .outForDeliveryAt(a.getOutForDeliveryAt())
                .arrivedAtDropAt(a.getArrivedAtDropAt())
                .deliveredAt(a.getDeliveredAt())
                .cancelledAt(cancelled ? a.getRespondedAt() : null)
                .cancellationReason(a.getCancellationReason())
                .baseFarePaise(delivered ? a.getBaseFarePaise() : 0)
                .distanceFarePaise(delivered ? a.getDistanceFarePaise() : 0)
                .incentivePaise(delivered ? a.getIncentivePaise() : 0)
                .totalPayoutPaise(delivered ? a.getEstimatedPayoutPaise() : 0)
                .paymentMethod(a.getPaymentMethod())
                .codAmountPaise(a.getCodAmountPaise())
                .build();
    }
}
