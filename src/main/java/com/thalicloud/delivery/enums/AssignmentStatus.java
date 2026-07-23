package com.thalicloud.delivery.enums;

// M4 — Delivery Request Handling.
public enum AssignmentStatus {
    OFFERED,               // FR-4.1 — pushed to the partner, countdown running
    ACCEPTED,               // FR-4.4 — partner is now ON_DELIVERY
    ARRIVED_AT_KITCHEN,    // M5/FR-5.4 — partner tapped "Arrived at Kitchen"
    PICKED_UP,              // M5/FR-5.6 — pickup code/QR verified
    OUT_FOR_DELIVERY,       // M6/FR-6.1 — proceeding to the drop location
    ARRIVED_AT_DROP,        // M6/FR-6.3 — partner tapped "Arrived at Drop Location"
    DELIVERED,               // M6/FR-6.6 — delivery OTP/contactless proof verified
    DECLINED,               // FR-4.4/FR-4.5 — partner rejected it
    EXPIRED,                // FR-4.3/FR-4.5 — countdown ran out with no action
    CANCELLED_BY_PARTNER,   // FR-4.8
    CANCELLED_BY_CUSTOMER,  // FR-4.7
    CANCELLED_BY_KITCHEN,   // FR-4.7
    COMPLETED
}
