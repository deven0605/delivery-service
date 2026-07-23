package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.DutyStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// M3.2 — FR-3.6/FR-3.8. todayDeliveries/todayEarningsPaise now reflect real
// DELIVERED assignments (M6) instead of always 0. activeDelivery is wired to
// a real in-flight DeliveryAssignment (M4-M6) instead of always-null.
@Getter
@Builder
public class DashboardSummaryResponse {
    private final DutyStatus dutyStatus;
    private final int todayDeliveries;
    private final long todayEarningsPaise;
    private final Double rating;
    // FR-10.3 — true once `rating` drops below the configurable advisory
    // threshold; drives the Home dashboard's advisory banner (no
    // auto-suspension in Phase 1).
    private final boolean lowRatingWarning;
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
        // M5/M6 — lets the Home dashboard route the "Active Delivery" card
        // straight into the right Pickup/Drop screen without a second round-trip.
        private final double kitchenLatitude;
        private final double kitchenLongitude;
        private final String kitchenContactNumber;
        private final double dropLatitude;
        private final double dropLongitude;
        private final String customerContactNumber;

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
                    .kitchenLatitude(a.getKitchenLatitude())
                    .kitchenLongitude(a.getKitchenLongitude())
                    .kitchenContactNumber(a.getKitchenContactNumber())
                    .dropLatitude(a.getDropLatitude())
                    .dropLongitude(a.getDropLongitude())
                    .customerContactNumber(a.getCustomerContactNumber())
                    .build();
        }
    }
}
