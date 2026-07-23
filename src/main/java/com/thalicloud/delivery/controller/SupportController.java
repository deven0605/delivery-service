package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreateSupportIssueRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.FaqResponse;
import com.thalicloud.delivery.dto.response.SupportConfigResponse;
import com.thalicloud.delivery.dto.response.SupportIssueResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// M13 — Help & Support (FR-13.1-FR-13.3). Same "/me" convention as
// DeliveryPartnerController — FAQs/config are static/global but still sit
// under the partner-authenticated base path rather than opening new public
// routes in SecurityConfig.
@RestController
@RequestMapping("/api/delivery/partners/me/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    /** GET /api/delivery/partners/me/support/faqs — FR-13.1. */
    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getFaqs() {
        return ResponseEntity.ok(ApiResponse.success("OK", supportService.getFaqs()));
    }

    /** GET /api/delivery/partners/me/support/config — FR-13.3 (SOS/call-support number). */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SupportConfigResponse>> getSupportConfig() {
        return ResponseEntity.ok(ApiResponse.success("OK", supportService.getSupportConfig()));
    }

    /** POST /api/delivery/partners/me/support/issues — FR-13.2. */
    @PostMapping("/issues")
    public ResponseEntity<ApiResponse<SupportIssueResponse>> reportIssue(
            @Valid @RequestBody CreateSupportIssueRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Issue reported",
                supportService.reportIssue(partner.getId(), request.getCategory(), request.getDescription(), request.getAssignmentId())));
    }

    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<SupportIssueResponse>>> getMyIssues(
            @AuthenticationPrincipal DeliveryPartner partner) {
        return ResponseEntity.ok(ApiResponse.success("OK", supportService.getMyIssues(partner.getId())));
    }
}
