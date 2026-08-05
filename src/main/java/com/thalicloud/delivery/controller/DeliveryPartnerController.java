package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.BankDetailsRequest;
import com.thalicloud.delivery.dto.request.DeviceTokenRequest;
import com.thalicloud.delivery.dto.request.DutyStatusRequest;
import com.thalicloud.delivery.dto.request.EditProfileRequest;
import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.RemitCashRequest;
import com.thalicloud.delivery.dto.request.UploadDocumentRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.CashInHandResponse;
import com.thalicloud.delivery.dto.response.DashboardSummaryResponse;
import com.thalicloud.delivery.dto.response.DeliveryFeedbackResponse;
import com.thalicloud.delivery.dto.response.DeliveryHistoryDetailResponse;
import com.thalicloud.delivery.dto.response.DeliveryHistoryItemResponse;
import com.thalicloud.delivery.dto.response.DocumentResponse;
import com.thalicloud.delivery.dto.response.EarningsSummaryResponse;
import com.thalicloud.delivery.dto.response.PageResponse;
import com.thalicloud.delivery.dto.response.PartnerProfileResponse;
import com.thalicloud.delivery.dto.response.PayoutResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.enums.EarningsPeriod;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// M2 — Registration & KYC Onboarding (FR-2.1 - FR-2.9).
// Every endpoint acts on the caller's own profile, resolved from the JWT
// (same "/me" convention as vendor-service's GET /api/vendor/me) — the app
// never needs to learn/pass its own partner id.
@Slf4j
@RestController
@RequestMapping("/api/delivery/partners/me")
@RequiredArgsConstructor
@Tag(name = "Delivery Partner Profile", description = "Self-service profile management for the authenticated delivery partner: onboarding/KYC, duty status, dashboard, cash-in-hand, earnings, payouts, and delivery history.")
public class DeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;

    @Operation(summary = "Get own profile", description = "Returns the authenticated partner's full profile, including onboarding/KYC status.")
    @GetMapping
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> getProfile(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getProfile: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getProfile(partner.getId())));
            log.info("getProfile: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getProfile: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Save personal details", description = "Saves the partner's personal onboarding details (name, DOB, address, etc.) along with a base64-encoded selfie photo, as part of KYC registration.")
    /** PUT /api/delivery/partners/me/personal-details — FR-2.1 (JSON: fields + base64 selfie). */
    @PutMapping("/personal-details")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> savePersonalDetails(
            @Valid @RequestBody PersonalDetailsRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("savePersonalDetails: start, partnerId={}", partner.getId());
        try {
            System.out.println("Saving personal details for partner ID: " + partner.getId());
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Personal details saved", deliveryPartnerService.savePersonalDetails(partner.getId(), request)));
            log.info("savePersonalDetails: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("savePersonalDetails: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Edit profile", description = "Updates optional profile fields (email and/or profile photo) for an already-onboarded partner.")
    /** PUT /api/delivery/partners/me/profile — FR-11.3 (email/profile photo, both optional). */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> editProfile(
            @Valid @RequestBody EditProfileRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("editProfile: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Profile updated", deliveryPartnerService.editProfile(partner.getId(), request)));
            log.info("editProfile: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("editProfile: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Save vehicle details", description = "Saves or updates the partner's vehicle information (type, registration, etc.) as part of onboarding.")
    /** PUT /api/delivery/partners/me/vehicle — FR-2.2/FR-2.3. */
    @PutMapping("/vehicle")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> saveVehicleDetails(
            @Valid @RequestBody VehicleDetailsRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("saveVehicleDetails: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Vehicle details saved", deliveryPartnerService.saveVehicleDetails(partner.getId(), request)));
            log.info("saveVehicleDetails: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("saveVehicleDetails: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Save bank/payout details", description = "Saves the partner's payout destination — either bank account details (account number, IFSC, holder name) or a UPI id.")
    /** PUT /api/delivery/partners/me/bank-details — FR-2.8 (bank trio or UPI id). */
    @PutMapping("/bank-details")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> saveBankDetails(
            @Valid @RequestBody BankDetailsRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("saveBankDetails: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Bank details saved", deliveryPartnerService.saveBankDetails(partner.getId(), request)));
            log.info("saveBankDetails: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("saveBankDetails: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Upload KYC document", description = "Uploads a base64-encoded KYC document (e.g. license, ID proof) of the given type/side to object storage and records it against the partner's profile.")
    /** POST /api/delivery/partners/me/documents — FR-2.4-FR-2.7 (JSON: type/side + base64 file). */
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("uploadDocument: start, partnerId={}, type={}", partner.getId(), request.getType());
        try {
            ResponseEntity<ApiResponse<DocumentResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Document uploaded",
                    deliveryPartnerService.uploadDocument(partner.getId(), request.getType(), request.getSide(), request.getFile())));
            log.info("uploadDocument: end, partnerId={}, type={}", partner.getId(), request.getType());
            return response;
        } catch (Exception e) {
            log.error("uploadDocument: failed, partnerId={}, type={}", partner.getId(), request.getType(), e);
            throw e;
        }
    }

    @Operation(summary = "List uploaded KYC documents", description = "Returns all KYC documents the partner has uploaded so far, along with their verification status.")
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listDocuments(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("listDocuments: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<List<DocumentResponse>>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.listDocuments(partner.getId())));
            log.info("listDocuments: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("listDocuments: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Submit application for review", description = "Marks the partner's onboarding application as complete and submits it for KYC/admin review, once all required details and documents are present.")
    /** POST /api/delivery/partners/me/submit — FR-2.9. */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> submit(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("submit: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Application submitted for review", deliveryPartnerService.submitApplication(partner.getId())));
            log.info("submit: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("submit: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Update duty (online/offline) status", description = "Toggles whether the partner is currently on-duty and available to receive new delivery assignments.")
    /** PUT /api/delivery/partners/me/duty-status — FR-3.1/FR-3.4. */
    @PutMapping("/duty-status")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> updateDutyStatus(
            @Valid @RequestBody DutyStatusRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("updateDutyStatus: start, partnerId={}, status={}", partner.getId(), request.getStatus());
        try {
            ResponseEntity<ApiResponse<PartnerProfileResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Duty status updated", deliveryPartnerService.updateDutyStatus(partner.getId(), request)));
            log.info("updateDutyStatus: end, partnerId={}, status={}", partner.getId(), request.getStatus());
            return response;
        } catch (Exception e) {
            log.error("updateDutyStatus: failed, partnerId={}, status={}", partner.getId(), request.getStatus(), e);
            throw e;
        }
    }

    @Operation(summary = "Register push notification device token", description = "Registers or updates the Expo push notification token for the partner's current device, used to deliver push notifications.")
    /** PUT /api/delivery/partners/me/device-token — M12/FR-12, registers this device's Expo push token. */
    @PutMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("registerDeviceToken: start, partnerId={}", partner.getId());
        try {
            deliveryPartnerService.updateDeviceToken(partner.getId(), request.getExpoPushToken());
            ResponseEntity<ApiResponse<Void>> response = ResponseEntity.ok(ApiResponse.success("Device token registered", null));
            log.info("registerDeviceToken: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("registerDeviceToken: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Get dashboard summary", description = "Returns the home-screen dashboard summary for the partner (e.g. today's deliveries/earnings snapshot and duty state).")
    /** GET /api/delivery/partners/me/dashboard-summary — FR-3.6/FR-3.8. */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getDashboardSummary: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<DashboardSummaryResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getDashboardSummary(partner.getId())));
            log.info("getDashboardSummary: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getDashboardSummary: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Get cash-in-hand balance", description = "Returns the total cash-on-delivery amount the partner is currently holding and owes to be remitted.")
    /** GET /api/delivery/partners/me/cash-in-hand — FR-7.3. */
    @GetMapping("/cash-in-hand")
    public ResponseEntity<ApiResponse<CashInHandResponse>> getCashInHand(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getCashInHand: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<CashInHandResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getCashInHand(partner.getId())));
            log.info("getCashInHand: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getCashInHand: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Remit collected cash", description = "Submits a request to remit the partner's held cash-in-hand via the given method, pending review.")
    /** POST /api/delivery/partners/me/cash-in-hand/remit — FR-7.3/FR-7.4. */
    @PostMapping("/cash-in-hand/remit")
    public ResponseEntity<ApiResponse<CashInHandResponse>> remitCash(
            @Valid @RequestBody RemitCashRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("remitCash: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<CashInHandResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Submitted for review", deliveryPartnerService.remitCash(partner.getId(), request.getMethod())));
            log.info("remitCash: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("remitCash: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Get earnings summary", description = "Returns the partner's earnings summary for the requested period (today, week, or month); returns 400 if period is not one of those values.")
    /** GET /api/delivery/partners/me/earnings?period=today|week|month — FR-8.1/FR-8.2. */
    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<EarningsSummaryResponse>> getEarningsSummary(
            @RequestParam(defaultValue = "today") String period,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getEarningsSummary: start, partnerId={}, period={}", partner.getId(), period);
        try {
            EarningsPeriod parsed;
            try {
                parsed = EarningsPeriod.valueOf(period.toUpperCase());
            } catch (IllegalArgumentException ex) {
                ResponseEntity<ApiResponse<EarningsSummaryResponse>> badResponse = ResponseEntity.badRequest()
                        .body(ApiResponse.error("period must be one of: today, week, month"));
                log.info("getEarningsSummary: end, partnerId={}, period={}, invalidPeriod=true", partner.getId(), period);
                return badResponse;
            }
            ResponseEntity<ApiResponse<EarningsSummaryResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getEarningsSummary(partner.getId(), parsed)));
            log.info("getEarningsSummary: end, partnerId={}, period={}", partner.getId(), period);
            return response;
        } catch (Exception e) {
            log.error("getEarningsSummary: failed, partnerId={}, period={}", partner.getId(), period, e);
            throw e;
        }
    }

    @Operation(summary = "Get payout history", description = "Returns the list of past payouts made to the partner.")
    /** GET /api/delivery/partners/me/payouts — FR-8.4. */
    @GetMapping("/payouts")
    public ResponseEntity<ApiResponse<List<PayoutResponse>>> getPayoutHistory(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getPayoutHistory: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<List<PayoutResponse>>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getPayoutHistory(partner.getId())));
            log.info("getPayoutHistory: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getPayoutHistory: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Get delivery history", description = "Returns a paginated list of the partner's past completed deliveries.")
    /** GET /api/delivery/partners/me/deliveries/history?page=0&size=20 — FR-9.1/FR-9.3. */
    @GetMapping("/deliveries/history")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryHistoryItemResponse>>> getDeliveryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getDeliveryHistory: start, partnerId={}, page={}, size={}", partner.getId(), page, size);
        try {
            ResponseEntity<ApiResponse<PageResponse<DeliveryHistoryItemResponse>>> response = ResponseEntity.ok(ApiResponse.success(
                    "OK", deliveryPartnerService.getDeliveryHistory(partner.getId(), page, size)));
            log.info("getDeliveryHistory: end, partnerId={}, page={}, size={}", partner.getId(), page, size);
            return response;
        } catch (Exception e) {
            log.error("getDeliveryHistory: failed, partnerId={}, page={}, size={}", partner.getId(), page, size, e);
            throw e;
        }
    }

    @Operation(summary = "Get delivery history detail", description = "Returns full details of one past delivery from the partner's history, identified by assignment id.")
    /** GET /api/delivery/partners/me/deliveries/history/{assignmentId} — FR-9.2. */
    @GetMapping("/deliveries/history/{assignmentId}")
    public ResponseEntity<ApiResponse<DeliveryHistoryDetailResponse>> getDeliveryHistoryDetail(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getDeliveryHistoryDetail: start, partnerId={}, assignmentId={}", partner.getId(), assignmentId);
        try {
            ResponseEntity<ApiResponse<DeliveryHistoryDetailResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "OK", deliveryPartnerService.getDeliveryHistoryDetail(partner.getId(), assignmentId)));
            log.info("getDeliveryHistoryDetail: end, partnerId={}, assignmentId={}", partner.getId(), assignmentId);
            return response;
        } catch (Exception e) {
            log.error("getDeliveryHistoryDetail: failed, partnerId={}, assignmentId={}", partner.getId(), assignmentId, e);
            throw e;
        }
    }

    @Operation(summary = "Get recent customer feedback", description = "Returns the partner's most recent delivery ratings and feedback from customers.")
    /** GET /api/delivery/partners/me/feedback — FR-10.2. */
    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<List<DeliveryFeedbackResponse>>> getRecentFeedback(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getRecentFeedback: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<List<DeliveryFeedbackResponse>>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getRecentFeedback(partner.getId())));
            log.info("getRecentFeedback: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getRecentFeedback: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }
}
