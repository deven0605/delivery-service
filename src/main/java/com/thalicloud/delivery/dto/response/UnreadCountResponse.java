package com.thalicloud.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

// M12 — Notifications (FR-12.2).
@Getter
@Builder
public class UnreadCountResponse {
    private final long unreadCount;
}
