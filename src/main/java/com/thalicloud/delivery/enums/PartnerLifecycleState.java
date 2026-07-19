package com.thalicloud.delivery.enums;

// Mirrors com.thalicloud.auth.enums.PartnerLifecycleState — same shared
// delivery_partners.lifecycle_state column, owned/created by auth-service.
public enum PartnerLifecycleState {
    PENDING_VERIFICATION,
    APPROVED,
    SUSPENDED
}
