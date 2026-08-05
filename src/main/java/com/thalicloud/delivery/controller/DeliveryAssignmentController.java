package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.ArrivalRequest;
import com.thalicloud.delivery.dto.request.CancellationRequest;
import com.thalicloud.delivery.dto.request.DropPhotoUploadRequest;
import com.thalicloud.delivery.dto.request.VerifyDeliveryRequest;
import com.thalicloud.delivery.dto.request.VerifyPickupRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// M4 — Delivery Request Handling (FR-4.1 - FR-4.8). Same "/me" convention as
// DeliveryPartnerController — the partner acted on is always the caller's own
// JWT-resolved id, never a path/body param.
@Slf4j
@RestController
@RequestMapping("/api/delivery/partners/me/assignments")
@RequiredArgsConstructor
@Tag(name = "Delivery Assignments", description = "Lifecycle of the authenticated partner's current delivery request: accept/decline, pickup and drop-off verification, cash collection, and cancellation.")
public class DeliveryAssignmentController {

    private final DeliveryAssignmentService assignmentService;

    @Operation(summary = "Get current assignment", description = "Returns the authenticated partner's in-progress assignment, if any, so the app can resume the correct screen after a restart or reconnect.")
    /** GET /api/delivery/partners/me/assignments/current — resume support. */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getCurrent(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getCurrent: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "OK", assignmentService.getCurrentAssignment(partner.getId()).orElse(null)));
            log.info("getCurrent: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getCurrent: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Accept a delivery request", description = "Accepts the incoming assignment offer identified by id on behalf of the authenticated partner.")
    /** POST /api/delivery/partners/me/assignments/{id}/accept — FR-4.4. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<AssignmentResponse>> accept(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("accept: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Request accepted", assignmentService.accept(partner.getId(), id)));
            log.info("accept: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("accept: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Decline a delivery request", description = "Declines the incoming assignment offer identified by id, freeing it up for reassignment to another partner.")
    /** POST /api/delivery/partners/me/assignments/{id}/decline — FR-4.4/FR-4.5. */
    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<AssignmentResponse>> decline(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("decline: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Request declined", assignmentService.decline(partner.getId(), id)));
            log.info("decline: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("decline: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Record arrival at the kitchen", description = "Marks the partner as arrived at the vendor/kitchen for pickup, optionally recording the GPS coordinates of the arrival.")
    /** POST /api/delivery/partners/me/assignments/{id}/arrive — FR-5.4. */
    @PostMapping("/{id}/arrive")
    public ResponseEntity<ApiResponse<AssignmentResponse>> arrive(
            @PathVariable UUID id,
            @RequestBody(required = false) ArrivalRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("arrive: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ArrivalRequest body = request != null ? request : new ArrivalRequest();
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Arrival recorded", assignmentService.arriveAtKitchen(partner.getId(), id, body.getLatitude(), body.getLongitude())));
            log.info("arrive: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("arrive: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Verify order pickup", description = "Confirms pickup of the order from the vendor using the pickup verification code, advancing the assignment to the drop-off phase.")
    /** POST /api/delivery/partners/me/assignments/{id}/verify-pickup — FR-5.5/FR-5.6. */
    @PostMapping("/{id}/verify-pickup")
    public ResponseEntity<ApiResponse<AssignmentResponse>> verifyPickup(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyPickupRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("verifyPickup: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Pickup verified", assignmentService.verifyPickup(partner.getId(), id, request.getCode())));
            log.info("verifyPickup: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("verifyPickup: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Start navigation to drop-off", description = "Marks the partner as heading to the customer's drop location after a successful pickup.")
    /** POST /api/delivery/partners/me/assignments/{id}/start-drop-navigation — FR-6.1. */
    @PostMapping("/{id}/start-drop-navigation")
    public ResponseEntity<ApiResponse<AssignmentResponse>> startDropNavigation(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("startDropNavigation: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Heading to drop location", assignmentService.startDropNavigation(partner.getId(), id)));
            log.info("startDropNavigation: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("startDropNavigation: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Record arrival at drop-off", description = "Marks the partner as arrived at the customer's drop-off location, optionally recording the GPS coordinates of the arrival.")
    /** POST /api/delivery/partners/me/assignments/{id}/arrive-drop — FR-6.3. */
    @PostMapping("/{id}/arrive-drop")
    public ResponseEntity<ApiResponse<AssignmentResponse>> arriveAtDrop(
            @PathVariable UUID id,
            @RequestBody(required = false) ArrivalRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("arriveAtDrop: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ArrivalRequest body = request != null ? request : new ArrivalRequest();
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Arrival recorded", assignmentService.arriveAtDrop(partner.getId(), id, body.getLatitude(), body.getLongitude())));
            log.info("arriveAtDrop: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("arriveAtDrop: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Upload proof-of-delivery photo", description = "Uploads a base64-encoded photo taken at the drop location as proof of delivery for contactless or unattended handoffs.")
    /** POST /api/delivery/partners/me/assignments/{id}/drop-photo — FR-6.5. */
    @PostMapping("/{id}/drop-photo")
    public ResponseEntity<ApiResponse<AssignmentResponse>> uploadDropPhoto(
            @PathVariable UUID id,
            @Valid @RequestBody DropPhotoUploadRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("uploadDropPhoto: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Proof-of-delivery photo uploaded",
                    assignmentService.uploadDropPhoto(partner.getId(), id,
                            request.getFile().getFileName(), request.getFile().getContentType(), request.getFile().decode())));
            log.info("uploadDropPhoto: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("uploadDropPhoto: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Mark cash collected", description = "Records that the partner has collected cash-on-delivery payment from the customer for this assignment.")
    /** POST /api/delivery/partners/me/assignments/{id}/collect-cash — FR-7.1/FR-7.2. */
    @PostMapping("/{id}/collect-cash")
    public ResponseEntity<ApiResponse<AssignmentResponse>> markCashCollected(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("markCashCollected: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Cash collected", assignmentService.markCashCollected(partner.getId(), id)));
            log.info("markCashCollected: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("markCashCollected: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Verify and complete delivery", description = "Confirms final delivery to the customer using the delivery OTP (or a contactless override flag), completing the assignment.")
    /** POST /api/delivery/partners/me/assignments/{id}/verify-delivery — FR-6.4/FR-6.5/FR-6.6/FR-6.7. */
    @PostMapping("/{id}/verify-delivery")
    public ResponseEntity<ApiResponse<AssignmentResponse>> verifyDelivery(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyDeliveryRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("verifyDelivery: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Delivery confirmed", assignmentService.verifyDelivery(partner.getId(), id, request.getOtp(), request.isContactless())));
            log.info("verifyDelivery: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("verifyDelivery: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }

    @Operation(summary = "Cancel an accepted delivery", description = "Requests cancellation of an already-accepted assignment on behalf of the partner, recording the given reason.")
    /** POST /api/delivery/partners/me/assignments/{id}/cancel — FR-4.8. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AssignmentResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancellationRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("cancel: start, partnerId={}, assignmentId={}", partner.getId(), id);
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Delivery cancelled", assignmentService.requestCancellation(partner.getId(), id, request.getReason())));
            log.info("cancel: end, partnerId={}, assignmentId={}", partner.getId(), id);
            return response;
        } catch (Exception e) {
            log.error("cancel: failed, partnerId={}, assignmentId={}", partner.getId(), id, e);
            throw e;
        }
    }
}
