package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreatePayoutRequest;
import com.thalicloud.delivery.dto.request.UpdatePayoutStatusRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.PayoutResponse;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// M8.2 — the seam a future Ops weekly-batch payout job would call (FR-8.5:
// "manual/batch processed by Ops" — there's no such job or Ops console
// anywhere in this workspace, same gap as InternalAssignmentController). No
// partner action ever creates a Payout row here; this is also the only way
// to exercise the Payout History screen today.
//
// Protected the same way as InternalAssignmentController — a shared header
// key rather than the partner JWT scheme, permitAll at the Spring Security
// layer and checked here instead. Phase 1 placeholder, same spirit as the
// hardcoded OTP.
@RestController
@RequestMapping("/api/delivery/internal/payouts")
@RequiredArgsConstructor
public class InternalPayoutController {

    private final DeliveryPartnerService deliveryPartnerService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @PostMapping
    public ResponseEntity<ApiResponse<PayoutResponse>> createPayout(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreatePayoutRequest request) {
        ResponseEntity<ApiResponse<PayoutResponse>> denied = requireValidKey(key);
        if (denied != null) return denied;

        return ResponseEntity.ok(ApiResponse.success("Payout created", deliveryPartnerService.createPayout(
                request.getPartnerId(), request.getAmountPaise(), request.getDestinationReference(),
                request.getPeriodStart(), request.getPeriodEnd())));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PayoutResponse>> updateStatus(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePayoutStatusRequest request) {
        ResponseEntity<ApiResponse<PayoutResponse>> denied = requireValidKey(key);
        if (denied != null) return denied;

        return ResponseEntity.ok(ApiResponse.success(
                "Payout status updated", deliveryPartnerService.updatePayoutStatus(id, request.getStatus())));
    }

    private ResponseEntity<ApiResponse<PayoutResponse>> requireValidKey(String key) {
        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid or missing X-Internal-Key"));
        }
        return null;
    }
}
