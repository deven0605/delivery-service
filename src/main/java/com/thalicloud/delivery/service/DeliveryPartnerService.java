package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.request.BankDetailsRequest;
import com.thalicloud.delivery.dto.request.Base64FileRequest;
import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
import com.thalicloud.delivery.dto.request.DutyStatusRequest;
import com.thalicloud.delivery.dto.response.DashboardSummaryResponse;
import com.thalicloud.delivery.dto.response.DocumentResponse;
import com.thalicloud.delivery.dto.response.PartnerProfileResponse;
import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentType;

import java.util.List;
import java.util.UUID;

public interface DeliveryPartnerService {

    PartnerProfileResponse getProfile(UUID partnerId);

    // FR-2.1
    PartnerProfileResponse savePersonalDetails(UUID partnerId, PersonalDetailsRequest request);

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
}
