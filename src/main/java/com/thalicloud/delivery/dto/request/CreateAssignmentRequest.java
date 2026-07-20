package com.thalicloud.delivery.dto.request;

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

    @PositiveOrZero(message = "estimatedDistanceKm must be >= 0")
    private double estimatedDistanceKm;

    @Positive(message = "itemCount must be > 0")
    private int itemCount;
}
