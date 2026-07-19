package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.dto.request.Base64FileRequest;
import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
import com.thalicloud.delivery.dto.response.DocumentResponse;
import com.thalicloud.delivery.dto.response.PartnerProfileResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.entity.KYCDocument;
import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentStatus;
import com.thalicloud.delivery.enums.DocumentType;
import com.thalicloud.delivery.enums.PartnerLifecycleState;
import com.thalicloud.delivery.exception.FileValidationException;
import com.thalicloud.delivery.exception.RegistrationIncompleteException;
import com.thalicloud.delivery.exception.ResourceNotFoundException;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import com.thalicloud.delivery.repository.KYCDocumentRepository;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import com.thalicloud.delivery.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    private record RequiredDoc(DocumentType type, DocumentSide side, String label) {}

    private static final List<RequiredDoc> ALWAYS_REQUIRED = List.of(
            new RequiredDoc(DocumentType.AADHAAR, DocumentSide.FRONT, "Aadhaar (front)"),
            new RequiredDoc(DocumentType.AADHAAR, DocumentSide.BACK, "Aadhaar (back)"),
            new RequiredDoc(DocumentType.PAN, null, "PAN Card")
    );

    private static final List<RequiredDoc> MOTORIZED_REQUIRED = List.of(
            new RequiredDoc(DocumentType.DRIVING_LICENSE, DocumentSide.FRONT, "Driving License (front)"),
            new RequiredDoc(DocumentType.DRIVING_LICENSE, DocumentSide.BACK, "Driving License (back)"),
            new RequiredDoc(DocumentType.VEHICLE_RC, null, "Vehicle RC")
    );

    private final DeliveryPartnerRepository partnerRepository;
    private final KYCDocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    @Override
    @Transactional(readOnly = true)
    public PartnerProfileResponse getProfile(UUID partnerId) {
        DeliveryPartner partner = findPartner(partnerId);
        return PartnerProfileResponse.from(partner, listDocuments(partnerId));
    }

    @Override
    @Transactional
    public PartnerProfileResponse savePersonalDetails(UUID partnerId, PersonalDetailsRequest request) {
        DeliveryPartner partner = findPartner(partnerId);

        // FR-2.1 — the selfie is required, unlike the rest of this step.
        Base64FileRequest selfie = request.getProfilePhoto();
        if (selfie == null) {
            throw new FileValidationException("A selfie photo is required.");
        }

        String objectKey = "partners/%s/selfie%s".formatted(partnerId, extensionOf(selfie.getFileName(), selfie.getContentType()));
        String photoUrl = documentStorageService.upload(objectKey, selfie.decode(), selfie.getContentType());

        partner.setName(request.getFullName());
        partner.setDob(request.getDob());
        partner.setGender(request.getGender());
        partner.setEmail(request.getEmail());
        partner.setProfilePhotoUrl(photoUrl);
        partner.setUpdatedAt(LocalDateTime.now());
        partnerRepository.save(partner);

        return PartnerProfileResponse.from(partner, listDocuments(partnerId));
    }

    @Override
    @Transactional
    public PartnerProfileResponse saveVehicleDetails(UUID partnerId, VehicleDetailsRequest request) {
        DeliveryPartner partner = findPartner(partnerId);

        // FR-2.3 — vehicle number is mandatory once the vehicle is motorized.
        if (request.getVehicleType().isMotorized()
                && (request.getVehicleNumber() == null || request.getVehicleNumber().isBlank())) {
            throw new IllegalArgumentException("Vehicle number is required for a motorized vehicle.");
        }

        partner.setVehicleType(request.getVehicleType());
        if (request.getVehicleType().isMotorized()) {
            partner.setVehicleNumber(request.getVehicleNumber().trim().toUpperCase());
            partner.setVehicleModel(request.getVehicleModel());
        } else {
            // Bicycle/On Foot — no registration to track.
            partner.setVehicleNumber(null);
            partner.setVehicleModel(null);
        }
        partner.setUpdatedAt(LocalDateTime.now());
        partnerRepository.save(partner);

        return PartnerProfileResponse.from(partner, listDocuments(partnerId));
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(UUID partnerId, DocumentType type, DocumentSide side, Base64FileRequest file) {
        DeliveryPartner partner = findPartner(partnerId);
        validateDocumentShape(partner, type, side);

        byte[] data = file.decode();
        String objectKey = "partners/%s/%s%s%s".formatted(
                partnerId, type, side != null ? "-" + side : "", extensionOf(file.getFileName(), file.getContentType()));
        String fileUrl = documentStorageService.upload(objectKey, data, file.getContentType());

        // FR-2.7 — retrying a failed/rejected upload replaces the prior attempt.
        KYCDocument doc = documentRepository.findByPartnerIdAndTypeAndSide(partnerId, type, side)
                .orElseGet(KYCDocument::new);
        doc.setPartner(partner);
        doc.setType(type);
        doc.setSide(side);
        doc.setFileUrl(fileUrl);
        doc.setFileName(file.getFileName());
        doc.setContentType(file.getContentType());
        doc.setFileSizeBytes((long) data.length);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setRejectReason(null);
        doc.setUploadedAt(LocalDateTime.now());
        documentRepository.save(doc);

        return DocumentResponse.from(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(UUID partnerId) {
        return documentRepository.findByPartnerId(partnerId).stream()
                .sorted(Comparator.comparing(KYCDocument::getUploadedAt))
                .map(DocumentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PartnerProfileResponse submitApplication(UUID partnerId) {
        DeliveryPartner partner = findPartner(partnerId);

        List<String> missing = new ArrayList<>();
        if (partner.getName() == null || partner.getName().isBlank() || partner.getDob() == null) {
            missing.add("Personal details");
        }
        if (partner.getVehicleType() == null) {
            missing.add("Vehicle details");
        }

        List<KYCDocument> uploaded = documentRepository.findByPartnerId(partnerId);
        List<RequiredDoc> required = new ArrayList<>(ALWAYS_REQUIRED);
        if (partner.getVehicleType() != null && partner.getVehicleType().isMotorized()) {
            required.addAll(MOTORIZED_REQUIRED);
        }
        for (RequiredDoc doc : required) {
            boolean present = uploaded.stream()
                    .anyMatch(u -> u.getType() == doc.type() && u.getSide() == doc.side());
            if (!present) {
                missing.add(doc.label());
            }
        }

        if (!missing.isEmpty()) {
            throw new RegistrationIncompleteException("Please complete: " + String.join(", ", missing));
        }

        // FR-2.9 — moves the partner into Ops review.
        partner.setLifecycleState(PartnerLifecycleState.PENDING_VERIFICATION);
        partner.setRegistrationComplete(true);
        partner.setUpdatedAt(LocalDateTime.now());
        partnerRepository.save(partner);

        return PartnerProfileResponse.from(partner, listDocuments(partnerId));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private DeliveryPartner findPartner(UUID partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));
    }

    private void validateDocumentShape(DeliveryPartner partner, DocumentType type, DocumentSide side) {
        boolean expectsSide = type == DocumentType.AADHAAR || type == DocumentType.DRIVING_LICENSE;
        if (expectsSide && side == null) {
            throw new IllegalArgumentException(type + " requires a front/back side.");
        }
        if (!expectsSide && side != null) {
            throw new IllegalArgumentException(type + " does not take a front/back side.");
        }
        boolean motorizedOnly = type == DocumentType.DRIVING_LICENSE || type == DocumentType.VEHICLE_RC;
        if (motorizedOnly && (partner.getVehicleType() == null || !partner.getVehicleType().isMotorized())) {
            throw new IllegalArgumentException(type + " is only required for motorized vehicles.");
        }
    }

    private String extensionOf(String fileName, String contentType) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.'));
        }
        if ("image/png".equals(contentType)) return ".png";
        if ("application/pdf".equals(contentType)) return ".pdf";
        return ".jpg";
    }
}
