package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.PayoutStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// M8.2 — internal/Ops-batch-facing; marks a PROCESSING payout PAID or FAILED.
@Getter
@Setter
public class UpdatePayoutStatusRequest {

    @NotNull(message = "status is required")
    private PayoutStatus status;
}
