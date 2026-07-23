package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.PartnerNotification;
import com.thalicloud.delivery.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// M12 — Notifications (FR-12.3).
@Getter
@Builder
public class NotificationResponse {
    private final UUID id;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String referenceId;
    private final boolean read;
    private final LocalDateTime createdAt;

    public static NotificationResponse from(PartnerNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
