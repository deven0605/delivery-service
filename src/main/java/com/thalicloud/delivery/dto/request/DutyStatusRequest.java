package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.DutyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FR-3.1/FR-3.4 — the partner may only request ONLINE/OFFLINE directly;
// ON_DELIVERY is rejected here and set only by the (future) assignment flow.
@Getter
@Setter
public class DutyStatusRequest {

    @NotNull(message = "Status is required")
    private DutyStatus status;
}
