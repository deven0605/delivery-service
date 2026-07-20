package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.DutyStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// M3.2 — FR-3.6/FR-3.8. todayDeliveries/todayEarningsPaise are still always 0:
// there is still no completed-order<->earnings model anywhere in the
// workspace (that's M5/M6). activeDelivery is now wired to a real ACCEPTED
// DeliveryAssignment (M4) instead of always-null.
@Getter
@Builder
public class DashboardSummaryResponse {
    private final DutyStatus dutyStatus;
    private final int todayDeliveries;
    private final long todayEarningsPaise;
    private final Double rating;
    private final ActiveDeliveryResponse activeDelivery;

    @Getter
    @Builder
    public static class ActiveDeliveryResponse {
        private final UUID assignmentId;
        private final String orderId;
        private final String status;
        private final String kitchenName;
        private final String dropLocality;
        private final long estimatedPayoutPaise;
        private final double estimatedDistanceKm;
        private final int itemCount;

        public static ActiveDeliveryResponse from(DeliveryAssignment a) {
            return ActiveDeliveryResponse.builder()
                    .assignmentId(a.getId())
                    .orderId(a.getOrderId())
                    .status(a.getStatus().name())
                    .kitchenName(a.getKitchenName())
                    .dropLocality(a.getDropLocality())
                    .estimatedPayoutPaise(a.getEstimatedPayoutPaise())
                    .estimatedDistanceKm(a.getEstimatedDistanceKm())
                    .itemCount(a.getItemCount())
                    .build();
        }
    }
}
