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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// M4 — Delivery Request Handling (FR-4.1 - FR-4.8). Same "/me" convention as
// DeliveryPartnerController — the partner acted on is always the caller's own
// JWT-resolved id, never a path/body param.
@RestController
@RequestMapping("/api/delivery/partners/me/assignments")
@RequiredArgsConstructor
public class DeliveryAssignmentController {

    private final DeliveryAssignmentService assignmentService;

    /** GET /api/delivery/partners/me/assignments/current — resume support. */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getCurrent(
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "OK", assignmentService.getCurrentAssignment(partner.getId()).orElse(null)));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/accept — FR-4.4. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<AssignmentResponse>> accept(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Request accepted", assignmentService.accept(partner.getId(), id)));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/decline — FR-4.4/FR-4.5. */
    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<AssignmentResponse>> decline(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Request declined", assignmentService.decline(partner.getId(), id)));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/arrive — FR-5.4. */
    @PostMapping("/{id}/arrive")
    public ResponseEntity<ApiResponse<AssignmentResponse>> arrive(
            @PathVariable UUID id,
            @RequestBody(required = false) ArrivalRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        ArrivalRequest body = request != null ? request : new ArrivalRequest();
        return ResponseEntity.ok(ApiResponse.success(
                "Arrival recorded", assignmentService.arriveAtKitchen(partner.getId(), id, body.getLatitude(), body.getLongitude())));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/verify-pickup — FR-5.5/FR-5.6. */
    @PostMapping("/{id}/verify-pickup")
    public ResponseEntity<ApiResponse<AssignmentResponse>> verifyPickup(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyPickupRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pickup verified", assignmentService.verifyPickup(partner.getId(), id, request.getCode())));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/start-drop-navigation — FR-6.1. */
    @PostMapping("/{id}/start-drop-navigation")
    public ResponseEntity<ApiResponse<AssignmentResponse>> startDropNavigation(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Heading to drop location", assignmentService.startDropNavigation(partner.getId(), id)));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/arrive-drop — FR-6.3. */
    @PostMapping("/{id}/arrive-drop")
    public ResponseEntity<ApiResponse<AssignmentResponse>> arriveAtDrop(
            @PathVariable UUID id,
            @RequestBody(required = false) ArrivalRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        ArrivalRequest body = request != null ? request : new ArrivalRequest();
        return ResponseEntity.ok(ApiResponse.success(
                "Arrival recorded", assignmentService.arriveAtDrop(partner.getId(), id, body.getLatitude(), body.getLongitude())));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/drop-photo — FR-6.5. */
    @PostMapping("/{id}/drop-photo")
    public ResponseEntity<ApiResponse<AssignmentResponse>> uploadDropPhoto(
            @PathVariable UUID id,
            @Valid @RequestBody DropPhotoUploadRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Proof-of-delivery photo uploaded",
                assignmentService.uploadDropPhoto(partner.getId(), id,
                        request.getFile().getFileName(), request.getFile().getContentType(), request.getFile().decode())));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/collect-cash — FR-7.1/FR-7.2. */
    @PostMapping("/{id}/collect-cash")
    public ResponseEntity<ApiResponse<AssignmentResponse>> markCashCollected(
            @PathVariable UUID id,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cash collected", assignmentService.markCashCollected(partner.getId(), id)));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/verify-delivery — FR-6.4/FR-6.5/FR-6.6/FR-6.7. */
    @PostMapping("/{id}/verify-delivery")
    public ResponseEntity<ApiResponse<AssignmentResponse>> verifyDelivery(
            @PathVariable UUID id,
            @Valid @RequestBody VerifyDeliveryRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Delivery confirmed", assignmentService.verifyDelivery(partner.getId(), id, request.getOtp(), request.isContactless())));
    }

    /** POST /api/delivery/partners/me/assignments/{id}/cancel — FR-4.8. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AssignmentResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancellationRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Delivery cancelled", assignmentService.requestCancellation(partner.getId(), id, request.getReason())));
    }
}
