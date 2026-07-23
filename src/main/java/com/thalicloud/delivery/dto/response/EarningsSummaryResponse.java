package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// M8.1 — Earnings Dashboard (FR-8.1/FR-8.2).
@Getter
@Builder
public class EarningsSummaryResponse {
    private final String period; // TODAY | WEEK | MONTH
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final long totalEarningsPaise;
    private final int deliveryCount;
    private final long averagePayoutPaise;
    private final List<DeliveryEarningResponse> deliveries;

    @Getter
    @Builder
    public static class DeliveryEarningResponse {
        private final UUID assignmentId;
        private final String orderId;
        private final String kitchenName;
        private final String dropLocality;
        private final LocalDateTime deliveredAt;
        private final long baseFarePaise;
        private final long distanceFarePaise;
        private final long incentivePaise;
        private final long totalPayoutPaise;

        public static DeliveryEarningResponse from(DeliveryAssignment a) {
            return DeliveryEarningResponse.builder()
                    .assignmentId(a.getId())
                    .orderId(a.getOrderId())
                    .kitchenName(a.getKitchenName())
                    .dropLocality(a.getDropLocality())
                    .deliveredAt(a.getDeliveredAt())
                    .baseFarePaise(a.getBaseFarePaise())
                    .distanceFarePaise(a.getDistanceFarePaise())
                    .incentivePaise(a.getIncentivePaise())
                    .totalPayoutPaise(a.getEstimatedPayoutPaise())
                    .build();
        }
    }
}
