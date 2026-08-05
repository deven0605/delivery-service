package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.CreateSupportIssueRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.FaqResponse;
import com.thalicloud.delivery.dto.response.SupportConfigResponse;
import com.thalicloud.delivery.dto.response.SupportIssueResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// M13 — Help & Support (FR-13.1-FR-13.3). Same "/me" convention as
// DeliveryPartnerController — FAQs/config are static/global but still sit
// under the partner-authenticated base path rather than opening new public
// routes in SecurityConfig.
@Slf4j
@RestController
@RequestMapping("/api/delivery/partners/me/support")
@RequiredArgsConstructor
@Tag(name = "Delivery Partner Support", description = "Help and support resources for the authenticated delivery partner: FAQs, support contact configuration, and reporting/tracking support issues.")
public class SupportController {

    private final SupportService supportService;

    @Operation(summary = "Get FAQs", description = "Returns the list of frequently asked questions shown to delivery partners in the help section.")
    /** GET /api/delivery/partners/me/support/faqs — FR-13.1. */
    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getFaqs() {
        log.info("getFaqs: start");
        try {
            ResponseEntity<ApiResponse<List<FaqResponse>>> response = ResponseEntity.ok(ApiResponse.success("OK", supportService.getFaqs()));
            log.info("getFaqs: end");
            return response;
        } catch (Exception e) {
            log.error("getFaqs: failed", e);
            throw e;
        }
    }

    @Operation(summary = "Get support configuration", description = "Returns support contact configuration for the app, such as the SOS/call-support phone number.")
    /** GET /api/delivery/partners/me/support/config — FR-13.3 (SOS/call-support number). */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SupportConfigResponse>> getSupportConfig() {
        log.info("getSupportConfig: start");
        try {
            ResponseEntity<ApiResponse<SupportConfigResponse>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", supportService.getSupportConfig()));
            log.info("getSupportConfig: end");
            return response;
        } catch (Exception e) {
            log.error("getSupportConfig: failed", e);
            throw e;
        }
    }

    @Operation(summary = "Report a support issue", description = "Files a new support issue on behalf of the authenticated partner, with a category, description, and optional related assignment id.")
    /** POST /api/delivery/partners/me/support/issues — FR-13.2. */
    @PostMapping("/issues")
    public ResponseEntity<ApiResponse<SupportIssueResponse>> reportIssue(
            @Valid @RequestBody CreateSupportIssueRequest request,
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("reportIssue: start, partnerId={}, category={}", partner.getId(), request.getCategory());
        try {
            ResponseEntity<ApiResponse<SupportIssueResponse>> response = ResponseEntity.ok(ApiResponse.success(
                    "Issue reported",
                    supportService.reportIssue(partner.getId(), request.getCategory(), request.getDescription(), request.getAssignmentId())));
            log.info("reportIssue: end, partnerId={}, category={}", partner.getId(), request.getCategory());
            return response;
        } catch (Exception e) {
            log.error("reportIssue: failed, partnerId={}, category={}", partner.getId(), request.getCategory(), e);
            throw e;
        }
    }

    @Operation(summary = "List my support issues", description = "Returns the support issues previously reported by the authenticated partner, along with their status.")
    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<SupportIssueResponse>>> getMyIssues(
            @AuthenticationPrincipal DeliveryPartner partner) {
        log.info("getMyIssues: start, partnerId={}", partner.getId());
        try {
            ResponseEntity<ApiResponse<List<SupportIssueResponse>>> response =
                    ResponseEntity.ok(ApiResponse.success("OK", supportService.getMyIssues(partner.getId())));
            log.info("getMyIssues: end, partnerId={}", partner.getId());
            return response;
        } catch (Exception e) {
            log.error("getMyIssues: failed, partnerId={}", partner.getId(), e);
            throw e;
        }
    }
}
