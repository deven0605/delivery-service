package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.CancelledBy;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FR-4.7 — internal/order-service-facing (see InternalAssignmentController).
@Getter
@Setter
public class SystemCancelRequest {

    @NotNull(message = "cancelledBy is required")
    private CancelledBy cancelledBy;
}
