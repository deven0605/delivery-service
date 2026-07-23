package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreateNotificationRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// M12 — the seam a future auth-service KYC-approval/rejection flow, or an Ops
// announcement tool, would call (FR-12.1's KYC_APPROVED/KYC_REJECTED/
// ANNOUNCEMENT types) — no such caller exists yet anywhere in the workspace,
// same gap as InternalAssignmentController/InternalPayoutController. The
// other notification types (NEW_ASSIGNMENT, ORDER_CANCELLED, PAYOUT_PROCESSED,
// RATING_ALERT) are created directly from inside delivery-service, at the
// point each event already happens.
//
// Protected the same way as the other Internal*Controllers — a shared header
// key, permitAll at the Spring Security layer and checked here instead.
@RestController
@RequestMapping("/api/delivery/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createNotification(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreateNotificationRequest request) {
        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid or missing X-Internal-Key"));
        }

        notificationService.notify(request.getPartnerId(), request.getType(), request.getTitle(), request.getBody(), request.getReferenceId());
        return ResponseEntity.ok(ApiResponse.success("Notification created", null));
    }
}
