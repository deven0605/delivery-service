package com.thalicloud.delivery.enums;

// M3.1 — mirrors com.thalicloud.auth.enums.DutyStatus. ON_DELIVERY is set by
// the (future) assignment flow, never directly by the partner — the
// duty-status endpoint only ever accepts ONLINE/OFFLINE (FR-3.1/FR-3.4).
public enum DutyStatus {
    OFFLINE,
    ONLINE,
    ON_DELIVERY
}
