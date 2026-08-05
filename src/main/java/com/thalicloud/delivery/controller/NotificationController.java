package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.NotificationResponse;
import com.thalicloud.delivery.dto.response.PageResponse;
import com.thalicloud.delivery.dto.response.UnreadCountResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// M12 — Notifications (FR-12.2/FR-12.3). Same "/me" convention as
// DeliveryPartnerController — every endpoint acts on the caller's own
// notifications, resolved from the JWT.
@Slf4j
@RestController
@RequestMapping("/api/delivery/partners/me/notifications")
@RequiredArgsConstructor
@Tag(name = "Delivery Partner Notifications", description = "In-app notifications for the authenticated delivery partner: listing, unread count, and marking as read.")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "List notifications", description = "Returns a paginated list of the authenticated partner's notifications, most recent first.")
    /** GET /api/delivery/partners/me/notifications?page=0&size=20 — FR-12.3. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getNotifications: start, partnerId={}, page={}, size={}", partner.getId(), page, size);
        try {
            ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> response = ResponseEntity.ok(ApiResponse.success(
                    "OK", notificationService.getNotifications(partner.getId(), page, size)));
            log.info("getNotifications: end, partnerId={}, page={}, size={}", partner.getId(), page, size);
            return response;
        } catch (Exception e) {
            log.error("getNotifications: failed, partnerId={}, page={}, size={}", partner.getId(), page, size, e);
            throw e;
        }
    }

    @Operation(summary = "Get unread notification count", description = "Returns the count of unread notifications for the authenticated partner, typically used for a badge indicator.")
    /** GET /api/delivery/partners/me/notifications/unread-count — FR-12.2. */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getUnreadCount: start, partnerId={}", partner.getId());
        try {
            long count = notificationService.getUnreadCount(partner.getId());
            ResponseEntity<ApiResponse<UnreadCountResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", UnreadCountResponse.builder().unreadCount(count).build()));
            log.info("getUnreadCount: end, partnerId={}, count={}", partner.getId(), count);
            return response;
        } catch (Exception e) {
            log.error("getUnreadCount: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Mark a notification as read", description = "Marks the specified notification, belonging to the authenticated partner, as read (e.g. on tap).")
    /** POST /api/delivery/partners/me/notifications/{id}/read — FR-12.3, tapping a notification. */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("markRead: start, partnerId={}, notificationId={}", partner.getId(), id);
        try {
            notificationService.markRead(partner.getId(), id);
            ResponseEntity<ApiResponse<Void>> response = ResponseEntity.ok(ApiResponse.success("Marked as read", null));
            log.info("markRead: end, partnerId={}, notificationId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("markRead: failed, partnerId={}, notificationId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks every unread notification belonging to the authenticated partner as read.")
    /** POST /api/delivery/partners/me/notifications/read-all */
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("markAllRead: start, partnerId={}", partner.getId());
        try {
            notificationService.markAllRead(partner.getId());
            ResponseEntity<ApiResponse<Void>> response = ResponseEntity.ok(ApiResponse.success("All marked as read", null));
            log.info("markAllRead: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("markAllRead: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }
}
