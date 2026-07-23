package com.thalicloud.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

// M7/FR-7.3 — the Cash in Hand balance + whether it has crossed the
// (configurable) remittance threshold.
@Getter
@Builder
public class CashInHandResponse {
    private final long cashInHandPaise;
    private final long remittanceThresholdPaise;
    private final boolean canRemit;
}
