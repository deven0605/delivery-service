package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.UploadDocumentRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.DocumentResponse;
import com.thalicloud.delivery.dto.response.PartnerProfileResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// M2 — Registration & KYC Onboarding (FR-2.1 - FR-2.9).
// Every endpoint acts on the caller's own profile, resolved from the JWT
// (same "/me" convention as vendor-service's GET /api/vendor/me) — the app
// never needs to learn/pass its own partner id.
@RestController
@RequestMapping("/api/delivery/partners/me")
@RequiredArgsConstructor
public class DeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> getProfile(
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.getProfile(partner.getId())));
    }

    /** PUT /api/delivery/partners/me/personal-details — FR-2.1 (JSON: fields + base64 selfie). */
    @PutMapping("/personal-details")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> savePersonalDetails(
            @Valid @RequestBody PersonalDetailsRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
                System.out.println("Saving personal details for partner ID: " + partner.getId());
        return ResponseEntity.ok(ApiResponse.success(
                "Personal details saved", deliveryPartnerService.savePersonalDetails(partner.getId(), request)));
    }

    /** PUT /api/delivery/partners/me/vehicle — FR-2.2/FR-2.3. */
    @PutMapping("/vehicle")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> saveVehicleDetails(
            @Valid @RequestBody VehicleDetailsRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Vehicle details saved", deliveryPartnerService.saveVehicleDetails(partner.getId(), request)));
    }

    /** POST /api/delivery/partners/me/documents — FR-2.4-FR-2.7 (JSON: type/side + base64 file). */
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Document uploaded",
                deliveryPartnerService.uploadDocument(partner.getId(), request.getType(), request.getSide(), request.getFile())));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listDocuments(
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success("OK", deliveryPartnerService.listDocuments(partner.getId())));
    }

    /** POST /api/delivery/partners/me/submit — FR-2.9. */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> submit(
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Application submitted for review", deliveryPartnerService.submitApplication(partner.getId())));
    }
}
