package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreateAssignmentRequest;
import com.thalicloud.delivery.dto.request.RateDeliveryRequest;
import com.thalicloud.delivery.dto.request.SystemCancelRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/delivery/internal/assignments")
@RequiredArgsConstructor
@Tag(name = "Internal - Assignments", description = "Internal service-to-service endpoints (not for external/UI clients) for creating and cancelling delivery assignments and recording delivery ratings. Guarded by a shared X-Internal-Key header rather than partner JWT auth.")
public class InternalAssignmentController {

    private final DeliveryAssignmentService assignmentService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @Operation(summary = "Create a delivery assignment", description = "Internal endpoint used by a dispatcher/order-service integration to create a new delivery assignment offer for a partner and order. Requires a valid X-Internal-Key header.")
    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreateAssignmentRequest request) {
        log.info("createAssignment: start, partnerId={}, orderId={}", request.getPartnerId(), request.getOrderId());
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> denied = requireValidKey(key);
            if (denied != null) {
                log.info("createAssignment: end, partnerId={}, orderId={}, denied=true", request.getPartnerId(), request.getOrderId());
                return denied;
            }

            ResponseEntity<ApiResponse<AssignmentResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("Assignment created", assignmentService.createAssignment(request)));
            log.info("createAssignment: end, partnerId={}, orderId={}", request.getPartnerId(), request.getOrderId());
            return response;
        } catch (Exception e) {
            log.error("createAssignment: failed, partnerId={}, orderId={}", request.getPartnerId(), request.getOrderId(), e);
            throw e;
        }
    }

    @Operation(summary = "System-cancel an assignment", description = "Internal endpoint used to force-cancel an assignment from outside the partner app (e.g. order cancelled upstream), recording who initiated the cancellation. Requires a valid X-Internal-Key header.")
    @PostMapping("/{id}/system-cancel")
    public ResponseEntity<ApiResponse<AssignmentResponse>> systemCancel(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id,
            @Valid @RequestBody SystemCancelRequest request) {
        log.info("systemCancel: start, assignmentId={}, cancelledBy={}", id, request.getCancelledBy());
        try {
            ResponseEntity<ApiResponse<AssignmentResponse>> denied = requireValidKey(key);
            if (denied != null) {
                log.info("systemCancel: end, assignmentId={}, denied=true", id);
                return denied;
            }

            ResponseEntity<ApiResponse<AssignmentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Assignment cancelled", assignmentService.systemCancel(id, request.getCancelledBy())));
            log.info("systemCancel: end, assignmentId={}", id);
            return response;
        } catch (Exception e) {
            log.error("systemCancel: failed, assignmentId={}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Record a delivery rating", description = "Internal endpoint used to attach a customer's rating and feedback text to a completed assignment. Requires a valid X-Internal-Key header.")
    /** POST /api/delivery/internal/assignments/{id}/rating — FR-10.1/FR-10.2. */
    @PostMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<Void>> rateDelivery(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id,
            @Valid @RequestBody RateDeliveryRequest request) {
        log.info("rateDelivery: start, assignmentId={}, rating={}", id, request.getRating());
        try {
            ResponseEntity<ApiResponse<Void>> denied = requireValidKey(key);
            if (denied != null) {
                log.info("rateDelivery: end, assignmentId={}, denied=true", id);
                return denied;
            }

            assignmentService.rateDelivery(id, request.getRating(), request.getFeedback());
            ResponseEntity<ApiResponse<Void>> response = ResponseEntity.ok(ApiResponse.success("Rating recorded", null));
            log.info("rateDelivery: end, assignmentId={}", id);
            return response;
        } catch (Exception e) {
            log.error("rateDelivery: failed, assignmentId={}", id, e);
            throw e;
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> requireValidKey(String key) {
        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid or missing X-Internal-Key"));
        }
        return null;
    }
}
