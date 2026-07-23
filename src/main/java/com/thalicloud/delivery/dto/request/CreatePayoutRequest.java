package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

// M8.2 — internal/Ops-batch-facing (see InternalPayoutController). No such
// caller exists yet anywhere in the workspace (same gap as
// CreateAssignmentRequest/InternalAssignmentController), so this is also the
// only way to exercise the Payout History screen today.
@Getter
@Setter
public class CreatePayoutRequest {

    @NotNull(message = "partnerId is required")
    private UUID partnerId;

    @Positive(message = "amountPaise must be > 0")
    private long amountPaise;

    @NotBlank(message = "destinationReference is required")
    private String destinationReference;

    @NotNull(message = "periodStart is required")
    private LocalDate periodStart;

    @NotNull(message = "periodEnd is required")
    private LocalDate periodEnd;
}
