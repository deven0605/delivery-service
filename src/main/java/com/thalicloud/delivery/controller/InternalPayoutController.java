package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreatePayoutRequest;
import com.thalicloud.delivery.dto.request.UpdatePayoutStatusRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.PayoutResponse;
import com.thalicloud.delivery.service.DeliveryPartnerService;
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
@Slf4j
@RestController
@RequestMapping("/api/delivery/internal/payouts")
@RequiredArgsConstructor
@Tag(name = "Internal - Payouts", description = "Internal service-to-service endpoints (not for external/UI clients), the seam a future Ops batch payout job would call to create payout records and update their status. Guarded by a shared X-Internal-Key header rather than partner JWT auth.")
public class InternalPayoutController {

    private final DeliveryPartnerService deliveryPartnerService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @Operation(summary = "Create a payout record", description = "Internal endpoint used to record a batch/manual payout of a given amount to a partner's destination account for a given period. Requires a valid X-Internal-Key header.")
    @PostMapping
    public ResponseEntity<ApiResponse<PayoutResponse>> createPayout(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody CreatePayoutRequest request) {
        log.info("createPayout: start, partnerId={}, amountPaise={}", request.getPartnerId(), request.getAmountPaise());
        try {
            ResponseEntity<ApiResponse<PayoutResponse>> denied = requireValidKey(key);
            if (denied != null) {
                log.info("createPayout: end, partnerId={}, denied=true", request.getPartnerId());
                return denied;
            }

            ResponseEntity<ApiResponse<PayoutResponse>> response = ResponseEntity.ok(ApiResponse.success("Payout created", deliveryPartnerService.createPayout(
                    request.getPartnerId(), request.getAmountPaise(), request.getDestinationReference(),
                    request.getPeriodStart(), request.getPeriodEnd())));
            log.info("createPayout: end, partnerId={}", request.getPartnerId());
            return response;
        } catch (Exception e) {
            log.error("createPayout: failed, partnerId={}", request.getPartnerId(), e);
            throw e;
        }
    }

    @Operation(summary = "Update payout status", description = "Internal endpoint used to update the processing status of an existing payout (e.g. mark as processed/failed). Requires a valid X-Internal-Key header.")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PayoutResponse>> updateStatus(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePayoutStatusRequest request) {
        log.info("updateStatus: start, payoutId={}, status={}", id, request.getStatus());
        try {
            ResponseEntity<ApiResponse<PayoutResponse>> denied = requireValidKey(key);
            if (denied != null) {
                log.info("updateStatus: end, payoutId={}, denied=true", id);
                return denied;
            }

            ResponseEntity<ApiResponse<PayoutResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Payout status updated", deliveryPartnerService.updatePayoutStatus(id, request.getStatus())));
            log.info("updateStatus: end, payoutId={}", id);
            return response;
        } catch (Exception e) {
            log.error("updateStatus: failed, payoutId={}", id, e);
            throw e;
        }
    }

    private ResponseEntity<ApiResponse<PayoutResponse>> requireValidKey(String key) {
        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid or missing X-Internal-Key"));
        }
        return null;
    }
}
