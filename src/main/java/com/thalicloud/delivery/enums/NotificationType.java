package com.thalicloud.delivery.enums;

// M12 — Notifications (FR-12.1).
public enum NotificationType {
    KYC_APPROVED,        // internal seam — see InternalNotificationController
    KYC_REJECTED,        // internal seam — see InternalNotificationController
    NEW_ASSIGNMENT,      // FR-4.1 backgrounded-fallback wake-up
    ORDER_CANCELLED,     // FR-4.7 system cancellation (customer/kitchen)
    PAYOUT_PROCESSED,    // FR-8.5 payout marked PAID
    RATING_ALERT,        // FR-10.3 rating dropped below the advisory threshold
    ANNOUNCEMENT         // internal seam — platform-wide notices
}
