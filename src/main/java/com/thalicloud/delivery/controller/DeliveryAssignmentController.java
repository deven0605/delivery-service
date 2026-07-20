package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CancellationRequest;
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
