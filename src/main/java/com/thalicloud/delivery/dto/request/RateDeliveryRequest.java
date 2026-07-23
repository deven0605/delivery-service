package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// M10 — Ratings & Feedback (FR-10.1/FR-10.2). Internal/order-service-facing
// (see InternalAssignmentController) — there's no customer-app integration
// in this workspace yet, same gap as SystemCancelRequest.
@Getter
@Setter
public class RateDeliveryRequest {

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 500, message = "feedback must be at most 500 characters")
    private String feedback;
}
