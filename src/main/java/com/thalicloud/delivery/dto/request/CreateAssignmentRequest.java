package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// M4.1 — internal/dispatcher-facing (see InternalAssignmentController). There
// is no real matching engine yet to fill these in automatically; a future
// dispatch service would compute kitchenDistanceKm/estimatedPayoutPaise/etc.
// and call this endpoint the same way this DTO's shape assumes.
@Getter
@Setter
public class CreateAssignmentRequest {

    @NotNull(message = "partnerId is required")
    private UUID partnerId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "kitchenName is required")
    private String kitchenName;

    @PositiveOrZero(message = "kitchenDistanceKm must be >= 0")
    private double kitchenDistanceKm;

    @NotBlank(message = "dropLocality is required")
    private String dropLocality;

    @Positive(message = "estimatedPayoutPaise must be > 0")
    private long estimatedPayoutPaise;

    // ── M8.1 — Earnings breakdown (FR-8.2). Must sum to estimatedPayoutPaise
    // (validated in the service layer, not declaratively). ──────────────────
    @PositiveOrZero(message = "baseFarePaise must be >= 0")
    private long baseFarePaise;

    @PositiveOrZero(message = "distanceFarePaise must be >= 0")
    private long distanceFarePaise;

    @PositiveOrZero(message = "incentivePaise must be >= 0")
    private long incentivePaise;

    @PositiveOrZero(message = "estimatedDistanceKm must be >= 0")
    private double estimatedDistanceKm;

    @Positive(message = "itemCount must be > 0")
    private int itemCount;

    // ── M5 — Pickup Navigation & Verification ───────────────────────────────
    @NotNull(message = "kitchenLatitude is required")
    @DecimalMin(value = "-90", message = "kitchenLatitude must be >= -90")
    @DecimalMax(value = "90", message = "kitchenLatitude must be <= 90")
    private Double kitchenLatitude;

    @NotNull(message = "kitchenLongitude is required")
    @DecimalMin(value = "-180", message = "kitchenLongitude must be >= -180")
    @DecimalMax(value = "180", message = "kitchenLongitude must be <= 180")
    private Double kitchenLongitude;

    @NotBlank(message = "kitchenContactNumber is required")
    private String kitchenContactNumber;

    // ── M6 — Drop Navigation & Delivery Confirmation ────────────────────────
    @NotNull(message = "dropLatitude is required")
    @DecimalMin(value = "-90", message = "dropLatitude must be >= -90")
    @DecimalMax(value = "90", message = "dropLatitude must be <= 90")
    private Double dropLatitude;

    @NotNull(message = "dropLongitude is required")
    @DecimalMin(value = "-180", message = "dropLongitude must be >= -180")
    @DecimalMax(value = "180", message = "dropLongitude must be <= 180")
    private Double dropLongitude;

    @NotBlank(message = "customerContactNumber is required")
    private String customerContactNumber;

    // ── M7 — Cash on Delivery (COD) Collection. codAmountPaise is required
    // (validated in the service layer, not declaratively) only when
    // paymentMethod is COD — that either/or isn't expressible here. ─────────
    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @Positive(message = "codAmountPaise must be > 0")
    private Long codAmountPaise;
}
