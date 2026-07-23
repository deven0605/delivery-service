package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.RemittanceMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FR-7.4 — the partner's chosen remittance channel; Phase 1 has no live UPI
// payout integration or support-point directory, so this is recorded as a
// declaration for manual Ops reconciliation, not verified against anything.
@Getter
@Setter
public class RemitCashRequest {

    @NotNull(message = "method is required")
    private RemittanceMethod method;
}
