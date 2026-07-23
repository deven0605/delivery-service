package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.NotificationResponse;
import com.thalicloud.delivery.dto.response.PageResponse;
import com.thalicloud.delivery.dto.response.UnreadCountResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// M12 — Notifications (FR-12.2/FR-12.3). Same "/me" convention as
// DeliveryPartnerController — every endpoint acts on the caller's own
// notifications, resolved from the JWT.
@RestController
@RequestMapping("/api/delivery/partners/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/delivery/partners/me/notifications?page=0&size=20 — FR-12.3. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "OK", notificationService.getNotifications(partner.getId(), page, size)));
    }

    /** GET /api/delivery/partners/me/notifications/unread-count — FR-12.2. */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal DeliveryPartner partner) {
        long count = notificationService.getUnreadCount(partner.getId());
        return ResponseEntity.ok(ApiResponse.success("OK", UnreadCountResponse.builder().unreadCount(count).build()));
    }

    /** POST /api/delivery/partners/me/notifications/{id}/read — FR-12.3, tapping a notification. */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        notificationService.markRead(partner.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    /** POST /api/delivery/partners/me/notifications/read-all */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal DeliveryPartner partner) {
        notificationService.markAllRead(partner.getId());
        return ResponseEntity.ok(ApiResponse.success("All marked as read", null));
    }
}
