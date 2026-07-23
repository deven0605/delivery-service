package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.response.NotificationResponse;
import com.thalicloud.delivery.dto.response.PageResponse;
import com.thalicloud.delivery.enums.NotificationType;

import java.util.UUID;

public interface NotificationService {

    // FR-12.1 — called from wherever the triggering event already happens in
    // this service (createAssignment, systemCancel, updatePayoutStatus,
    // rateDelivery) or via the internal seam (KYC approval/rejection,
    // platform announcements — see InternalNotificationController).
    void notify(UUID partnerId, NotificationType type, String title, String body, String referenceId);

    // FR-12.3 — most recent first, paginated.
    PageResponse<NotificationResponse> getNotifications(UUID partnerId, int page, int size);

    // FR-12.2
    long getUnreadCount(UUID partnerId);

    // FR-12.3 — tapping a notification marks it read.
    void markRead(UUID partnerId, UUID notificationId);

    void markAllRead(UUID partnerId);
}
