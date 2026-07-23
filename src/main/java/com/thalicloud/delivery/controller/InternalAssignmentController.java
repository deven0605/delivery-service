package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreateAssignmentRequest;
import com.thalicloud.delivery.dto.request.RateDeliveryRequest;
import com.thalicloud.delivery.dto.request.SystemCancelRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// M4 — the seam a future dispatch/order-service integration would call:
// creating an offer (FR-4.1) and pushing a system-side cancellation alert
// (FR-4.7). No such caller exists yet anywhere in the workspace, so this is
// also the only way to exercise the Incoming Request / Assignment
// Cancellation screens today (e.g. via a manual test call).
//
// Protected by a shared header key rather than the partner JWT scheme (a
// dispatcher isn't a delivery partner) — see SecurityConfig's PUBLIC_ENDPOINTS
// comment for why this is permitAll at the Spring Security layer and checked
// here instead. This is a Phase 1 placeholder, same spirit as the hardcoded
// OTP: in production this route wouldn't be reachable from the public
// gateway at all.
@RestController
@RequestMapping("/api/delivery/internal/assignments")
@RequiredArgsConstructor
public class InternalAssignmentController {

    private final DeliveryAssignmentService assignmentService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreateAssignmentRequest request) {
        ResponseEntity<ApiResponse<AssignmentResponse>> denied = requireValidKey(key);
        if (denied != null) return denied;

        return ResponseEntity.ok(ApiResponse.success("Assignment created", assignmentService.createAssignment(request)));
    }

    @PostMapping("/{id}/system-cancel")
    public ResponseEntity<ApiResponse<AssignmentResponse>> systemCancel(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id,
            @Valid @RequestBody SystemCancelRequest request) {
        ResponseEntity<ApiResponse<AssignmentResponse>> denied = requireValidKey(key);
        if (denied != null) return denied;

        return ResponseEntity.ok(ApiResponse.success(
                "Assignment cancelled", assignmentService.systemCancel(id, request.getCancelledBy())));
    }

    /** POST /api/delivery/internal/assignments/{id}/rating — FR-10.1/FR-10.2. */
    @PostMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<Void>> rateDelivery(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id,
            @Valid @RequestBody RateDeliveryRequest request) {
        ResponseEntity<ApiResponse<Void>> denied = requireValidKey(key);
        if (denied != null) return denied;

        assignmentService.rateDelivery(id, request.getRating(), request.getFeedback());
        return ResponseEntity.ok(ApiResponse.success("Rating recorded", null));
    }

    private <T> ResponseEntity<ApiResponse<T>> requireValidKey(String key) {
        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid or missing X-Internal-Key"));
        }
        return null;
    }
}
