package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.AssignmentStatus;
import com.thalicloud.delivery.enums.PaymentMethod;
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
    // ── M5 — Pickup Navigation & Verification. pickupCode is deliberately
    // NOT exposed here — FR-5.5 requires it to come from the kitchen
    // (Vendor app/portal or its QR code), never from this API. ──────────────
    private final double kitchenLatitude;
    private final double kitchenLongitude;
    private final String kitchenContactNumber;
    private final LocalDateTime arrivedAtKitchenAt;
    private final LocalDateTime pickedUpAt;
    // ── M6 — Drop Navigation & Delivery Confirmation. deliveryOtp is
    // deliberately NOT exposed here — FR-6.4 requires it to reach the
    // customer via SMS/push, never via this partner-facing API. ────────────
    private final double dropLatitude;
    private final double dropLongitude;
    private final String customerContactNumber;
    private final LocalDateTime outForDeliveryAt;
    private final LocalDateTime arrivedAtDropAt;
    private final boolean contactlessDrop;
    private final String proofOfDeliveryPhotoUrl;
    private final LocalDateTime deliveredAt;
    // ── M7 — Cash on Delivery (COD) Collection ──────────────────────────────
    private final PaymentMethod paymentMethod;
    private final Long codAmountPaise;
    private final boolean codCollected;
    private final LocalDateTime codCollectedAt;
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
                .kitchenLatitude(a.getKitchenLatitude())
                .kitchenLongitude(a.getKitchenLongitude())
                .kitchenContactNumber(a.getKitchenContactNumber())
                .arrivedAtKitchenAt(a.getArrivedAtKitchenAt())
                .pickedUpAt(a.getPickedUpAt())
                .dropLatitude(a.getDropLatitude())
                .dropLongitude(a.getDropLongitude())
                .customerContactNumber(a.getCustomerContactNumber())
                .outForDeliveryAt(a.getOutForDeliveryAt())
                .arrivedAtDropAt(a.getArrivedAtDropAt())
                .contactlessDrop(a.isContactlessDrop())
                .proofOfDeliveryPhotoUrl(a.getProofOfDeliveryPhotoUrl())
                .deliveredAt(a.getDeliveredAt())
                .paymentMethod(a.getPaymentMethod())
                .codAmountPaise(a.getCodAmountPaise())
                .codCollected(a.isCodCollected())
                .codCollectedAt(a.getCodCollectedAt())
                .status(a.getStatus())
                .offeredAt(a.getOfferedAt())
                .expiresAt(a.getExpiresAt())
                .build();
    }
}
