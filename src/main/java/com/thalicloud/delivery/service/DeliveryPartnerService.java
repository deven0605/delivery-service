package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.request.BankDetailsRequest;
import com.thalicloud.delivery.dto.request.Base64FileRequest;
import com.thalicloud.delivery.dto.request.EditProfileRequest;
import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
import com.thalicloud.delivery.dto.request.DutyStatusRequest;
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
import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentType;
import com.thalicloud.delivery.enums.EarningsPeriod;
import com.thalicloud.delivery.enums.PayoutStatus;
import com.thalicloud.delivery.enums.RemittanceMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DeliveryPartnerService {

    PartnerProfileResponse getProfile(UUID partnerId);

    // FR-2.1
    PartnerProfileResponse savePersonalDetails(UUID partnerId, PersonalDetailsRequest request);

    // M11/FR-11.3 — post-registration edit of only email/profile photo; unlike
    // savePersonalDetails, both fields are optional and independent.
    PartnerProfileResponse editProfile(UUID partnerId, EditProfileRequest request);

    // FR-2.2/FR-2.3
    PartnerProfileResponse saveVehicleDetails(UUID partnerId, VehicleDetailsRequest request);

    // FR-2.8 — either the bank trio or a UPI id is sufficient.
    PartnerProfileResponse saveBankDetails(UUID partnerId, BankDetailsRequest request);

    // FR-2.4-2.7 — re-upload of the same (type, side) replaces the prior attempt.
    DocumentResponse uploadDocument(UUID partnerId, DocumentType type, DocumentSide side, Base64FileRequest file);

    List<DocumentResponse> listDocuments(UUID partnerId);

    // FR-2.9 — validates required docs for the selected vehicle type before
    // flipping the partner to PENDING_VERIFICATION.
    PartnerProfileResponse submitApplication(UUID partnerId);

    // FR-3.1/FR-3.4 — only APPROVED partners may go ONLINE; can't go OFFLINE
    // while ON_DELIVERY.
    PartnerProfileResponse updateDutyStatus(UUID partnerId, DutyStatusRequest request);

    // FR-3.3 — persists the latest GPS fix published over STOMP.
    void updateLocation(UUID partnerId, double latitude, double longitude);

    // FR-3.6/FR-3.8
    DashboardSummaryResponse getDashboardSummary(UUID partnerId);

    // FR-7.3
    CashInHandResponse getCashInHand(UUID partnerId);

    // FR-7.3/FR-7.4 — rejects if the balance hasn't crossed the configured
    // threshold yet; on success, remits (debits) the full balance and records
    // a CashRemittance row for later Ops reconciliation.
    CashInHandResponse remitCash(UUID partnerId, RemittanceMethod method);

    // FR-8.1/FR-8.2
    EarningsSummaryResponse getEarningsSummary(UUID partnerId, EarningsPeriod period);

    // FR-8.4
    List<PayoutResponse> getPayoutHistory(UUID partnerId);

    // FR-8.5 — internal/Ops-batch-facing (see InternalPayoutController); starts PROCESSING.
    PayoutResponse createPayout(UUID partnerId, long amountPaise, String destinationReference, LocalDate periodStart, LocalDate periodEnd);

    // FR-8.5 — internal/Ops-batch-facing; marks a payout PAID or FAILED.
    PayoutResponse updatePayoutStatus(UUID payoutId, PayoutStatus status);

    // M9/FR-9.1/FR-9.3 — completed/cancelled deliveries only, 20 per page by default.
    PageResponse<DeliveryHistoryItemResponse> getDeliveryHistory(UUID partnerId, int page, int size);

    // M9/FR-9.2
    DeliveryHistoryDetailResponse getDeliveryHistoryDetail(UUID partnerId, UUID assignmentId);

    // M10/FR-10.2 — most recent rated deliveries, read-only.
    List<DeliveryFeedbackResponse> getRecentFeedback(UUID partnerId);
}
