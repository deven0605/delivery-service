package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.dto.response.FaqResponse;
import com.thalicloud.delivery.dto.response.SupportConfigResponse;
import com.thalicloud.delivery.dto.response.SupportIssueResponse;
import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.entity.SupportIssue;
import com.thalicloud.delivery.enums.IssueCategory;
import com.thalicloud.delivery.exception.ResourceNotFoundException;
import com.thalicloud.delivery.repository.DeliveryAssignmentRepository;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import com.thalicloud.delivery.repository.SupportIssueRepository;
import com.thalicloud.delivery.service.SupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    // FR-13.1 — static content covering payouts, COD remittance, document
    // re-upload, and app permissions, plus a couple of general topics. No
    // admin/CMS exists in this workspace to author these dynamically, same
    // "Phase 1 gap" as everywhere else — a future Ops tool would replace this
    // with a real table.
    private static final List<FaqResponse> FAQS = List.of(
            FaqResponse.builder().id("payout-schedule").category("Payouts")
                    .question("When do I get paid?")
                    .answer("Payouts are processed weekly by ThaliCloud Ops for all DELIVERED orders in that period. Check Earnings > View Payout History for each cycle's status.")
                    .build(),
            FaqResponse.builder().id("payout-destination").category("Payouts")
                    .question("How do I change where my payout is sent?")
                    .answer("Go to Profile > Edit Bank/UPI Details. The new destination takes effect from your next payout cycle, not the current one.")
                    .build(),
            FaqResponse.builder().id("cod-remittance").category("COD Remittance")
                    .question("What do I do with cash I collect on COD orders?")
                    .answer("Cash you collect adds to your Cash in Hand balance on the Earnings screen. Once it crosses the remittance threshold, you can remit it via UPI or hand it over at a support point.")
                    .build(),
            FaqResponse.builder().id("cod-threshold").category("COD Remittance")
                    .question("Why can't I remit my Cash in Hand yet?")
                    .answer("Remittance unlocks once your collected cash crosses the platform's configured threshold, shown as a progress bar on the Earnings screen.")
                    .build(),
            FaqResponse.builder().id("doc-reupload").category("Documents")
                    .question("My document was rejected. What now?")
                    .answer("Open Profile > Documents. Any rejected document shows the reject reason and a Re-upload action — a fresh upload replaces the old one and resets it to Pending review.")
                    .build(),
            FaqResponse.builder().id("doc-verification-time").category("Documents")
                    .question("How long does document verification take?")
                    .answer("Documents move from Pending to Verified (or Rejected with a reason) once Ops reviews them. There's no fixed SLA in Phase 1 — check back on the Documents tab.")
                    .build(),
            FaqResponse.builder().id("app-permissions").category("App Permissions")
                    .question("Why does the app need location access?")
                    .answer("Background location is required to receive nearby delivery offers and to share your live position with the customer while a delivery is active. You can grant it from your phone's Settings > Apps > ThaliCloud Partner > Permissions.")
                    .build(),
            FaqResponse.builder().id("app-permissions-camera").category("App Permissions")
                    .question("Why does the app need camera access?")
                    .answer("Camera access is used for your KYC document photos, profile photo, and Contactless Drop proof-of-delivery photos.")
                    .build()
    );

    private final SupportIssueRepository supportIssueRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryAssignmentRepository assignmentRepository;

    @Value("${support.phone-number}")
    private String supportPhoneNumber;

    @Value("${support.email}")
    private String supportEmail;

    @Override
    public List<FaqResponse> getFaqs() {
        log.info("getFaqs: start");
        try {
            log.info("getFaqs: end, count={}", FAQS.size());
            return FAQS;
        } catch (Exception e) {
            log.error("getFaqs: failed", e);
            throw e;
        }
    }

    @Override
    public SupportConfigResponse getSupportConfig() {
        log.info("getSupportConfig: start");
        try {
            SupportConfigResponse response = SupportConfigResponse.builder()
                    .supportPhoneNumber(supportPhoneNumber)
                    .supportEmail(supportEmail)
                    .build();
            log.info("getSupportConfig: end");
            return response;
        } catch (Exception e) {
            log.error("getSupportConfig: failed", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public SupportIssueResponse reportIssue(UUID partnerId, IssueCategory category, String description, UUID assignmentId) {
        log.info("reportIssue: start, partnerId={}, category={}", partnerId, category);
        try {
            DeliveryPartner partner = partnerRepository.findById(partnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

            // FR-13.2 — "tied to a specific delivery (optional)"; when supplied it
            // must actually belong to this partner.
            if (assignmentId != null) {
                DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));
                if (!assignment.getPartner().getId().equals(partnerId)) {
                    throw new IllegalArgumentException("Delivery not found");
                }
            }

            SupportIssue issue = new SupportIssue();
            issue.setPartner(partner);
            issue.setCategory(category);
            issue.setDescription(description);
            issue.setAssignmentId(assignmentId);
            supportIssueRepository.save(issue);

            SupportIssueResponse response = SupportIssueResponse.from(issue);
            log.info("reportIssue: end, partnerId={}, category={}", partnerId, category);
            return response;
        } catch (Exception e) {
            log.error("reportIssue: failed, partnerId={}, category={}", partnerId, category, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportIssueResponse> getMyIssues(UUID partnerId) {
        log.info("getMyIssues: start, partnerId={}", partnerId);
        try {
            List<SupportIssueResponse> response = supportIssueRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                    .map(SupportIssueResponse::from)
                    .toList();
            log.info("getMyIssues: end, partnerId={}", partnerId);
            return response;
        } catch (Exception e) {
            log.error("getMyIssues: failed, partnerId={}", partnerId, e);
            throw e;
        }
    }
}
