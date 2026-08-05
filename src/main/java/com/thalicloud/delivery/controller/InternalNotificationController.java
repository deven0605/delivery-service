package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreateNotificationRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/delivery/internal/notifications")
@RequiredArgsConstructor
@Tag(name = "Internal - Notifications", description = "Internal service-to-service endpoint (not for external/UI clients) for other services (e.g. auth-service KYC approval/rejection, Ops announcements) to push a notification to a delivery partner. Guarded by a shared X-Internal-Key header rather than partner JWT auth.")
public class InternalNotificationController {

    private final NotificationService notificationService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @Operation(summary = "Create a notification for a partner", description = "Internal endpoint used by other services to create and push a notification (e.g. KYC approved/rejected, announcement) to a specific delivery partner. Requires a valid X-Internal-Key header.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createNotification(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreateNotificationRequest request) {
        log.info("createNotification: start, partnerId={}, type={}", request.getPartnerId(), request.getType());
        try {
            if (key == null || !key.equals(dispatchKey)) {
                ResponseEntity<ApiResponse<Void>> denied = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or missing X-Internal-Key"));
                log.info("createNotification: end, partnerId={}, type={}, denied=true", request.getPartnerId(), request.getType());
                return denied;
            }

            notificationService.notify(request.getPartnerId(), request.getType(), request.getTitle(), request.getBody(), request.getReferenceId());
            ResponseEntity<ApiResponse<Void>> response = ResponseEntity.ok(ApiResponse.success("Notification created", null));
            log.info("createNotification: end, partnerId={}, type={}", request.getPartnerId(), request.getType());
            return response;
        } catch (Exception e) {
            log.error("createNotification: failed, partnerId={}, type={}", request.getPartnerId(), request.getType(), e);
            throw e;
        }
    }
}
