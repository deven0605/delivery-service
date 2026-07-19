package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.request.Base64FileRequest;
import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
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

    // FR-2.4-2.7 — re-upload of the same (type, side) replaces the prior attempt.
    DocumentResponse uploadDocument(UUID partnerId, DocumentType type, DocumentSide side, Base64FileRequest file);

    List<DocumentResponse> listDocuments(UUID partnerId);

    // FR-2.9 — validates required docs for the selected vehicle type before
    // flipping the partner to PENDING_VERIFICATION.
    PartnerProfileResponse submitApplication(UUID partnerId);
}
