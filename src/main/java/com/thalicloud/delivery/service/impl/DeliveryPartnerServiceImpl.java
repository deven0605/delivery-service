package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.dto.request.BankDetailsRequest;
import com.thalicloud.delivery.dto.request.Base64FileRequest;
import com.thalicloud.delivery.dto.request.DutyStatusRequest;
import com.thalicloud.delivery.dto.request.EditProfileRequest;
import com.thalicloud.delivery.dto.request.PersonalDetailsRequest;
import com.thalicloud.delivery.dto.request.VehicleDetailsRequest;
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
import com.thalicloud.delivery.entity.CashRemittance;
import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.entity.KYCDocument;
import com.thalicloud.delivery.entity.Payout;
import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentStatus;
import com.thalicloud.delivery.enums.DocumentType;
import com.thalicloud.delivery.enums.AssignmentStatus;
import com.thalicloud.delivery.enums.DutyStatus;
import com.thalicloud.delivery.enums.EarningsPeriod;
import com.thalicloud.delivery.enums.NotificationType;
import com.thalicloud.delivery.enums.PartnerLifecycleState;
import com.thalicloud.delivery.enums.PayoutStatus;
import com.thalicloud.delivery.enums.RemittanceMethod;
import com.thalicloud.delivery.exception.FileValidationException;
import com.thalicloud.delivery.exception.RegistrationIncompleteException;
import com.thalicloud.delivery.exception.ResourceNotFoundException;
import com.thalicloud.delivery.repository.CashRemittanceRepository;
import com.thalicloud.delivery.repository.DeliveryAssignmentRepository;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import com.thalicloud.delivery.repository.KYCDocumentRepository;
import com.thalicloud.delivery.repository.PayoutRepository;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import com.thalicloud.delivery.service.DocumentStorageService;
import com.thalicloud.delivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    // M5/M6 — the Home dashboard's "Active Delivery" card stays visible through
    // the whole pickup+drop flow, not just while still ACCEPTED. DELIVERED is
    // excluded — that's a one-time Delivery Summary screen, not a card.
    private static final List<AssignmentStatus> DASHBOARD_ACTIVE_STATUSES = List.of(
            AssignmentStatus.ACCEPTED, AssignmentStatus.ARRIVED_AT_KITCHEN, AssignmentStatus.PICKED_UP,
            AssignmentStatus.OUT_FOR_DELIVERY, AssignmentStatus.ARRIVED_AT_DROP);

    // M9 — Delivery History (FR-9.1). DECLINED/EXPIRED are excluded: the
    // partner never engaged with those offers, so they aren't "deliveries".
    private static final List<AssignmentStatus> HISTORY_STATUSES = List.of(
            AssignmentStatus.DELIVERED, AssignmentStatus.CANCELLED_BY_PARTNER,
            AssignmentStatus.CANCELLED_BY_CUSTOMER, AssignmentStatus.CANCELLED_BY_KITCHEN);

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
    private final DeliveryAssignmentRepository assignmentRepository;
    private final CashRemittanceRepository remittanceRepository;
    private final PayoutRepository payoutRepository;
    private final NotificationService notificationService;

    @Value("${cash.remittance-threshold-paise}")
    private long remittanceThresholdPaise;

    // FR-10.3 — configurable advisory threshold; below this the Home
    // dashboard shows a warning banner (no auto-suspension in Phase 1).
    @Value("${rating.low-threshold}")
    private double lowRatingThreshold;

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
    public PartnerProfileResponse editProfile(UUID partnerId, EditProfileRequest request) {
        DeliveryPartner partner = findPartner(partnerId);

        // FR-11.3 — both fields optional and independent; unlike
        // savePersonalDetails there's no full-form requirement here.
        if (request.getEmail() != null) {
            partner.setEmail(request.getEmail());
        }
        if (request.getProfilePhoto() != null) {
            Base64FileRequest photo = request.getProfilePhoto();
            String objectKey = "partners/%s/selfie%s".formatted(partnerId, extensionOf(photo.getFileName(), photo.getContentType()));
            String photoUrl = documentStorageService.upload(objectKey, photo.decode(), photo.getContentType());
            partner.setProfilePhotoUrl(photoUrl);
        }
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
    public PartnerProfileResponse saveBankDetails(UUID partnerId, BankDetailsRequest request) {
        DeliveryPartner partner = findPartner(partnerId);

        boolean hasUpi = request.getUpiId() != null && !request.getUpiId().isBlank();
        boolean hasBankTrio = request.getAccountHolderName() != null && !request.getAccountHolderName().isBlank()
                && request.getAccountNumber() != null && !request.getAccountNumber().isBlank()
                && request.getIfscCode() != null && !request.getIfscCode().isBlank();

        // FR-2.8 — either the bank trio or a UPI id is sufficient.
        if (!hasUpi && !hasBankTrio) {
            throw new IllegalArgumentException(
                    "Provide either a UPI ID or Account Holder Name, Account Number and IFSC Code.");
        }

        if (hasBankTrio) {
            partner.setBankAccountHolderName(request.getAccountHolderName().trim());
            partner.setBankAccountNumber(request.getAccountNumber().trim());
            partner.setBankIfscCode(request.getIfscCode().trim().toUpperCase());
        } else {
            partner.setBankAccountHolderName(null);
            partner.setBankAccountNumber(null);
            partner.setBankIfscCode(null);
        }
        partner.setUpiId(hasUpi ? request.getUpiId().trim() : null);
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
        // FR-2.8 — either the bank trio or a UPI id must be on file before submitting.
        boolean hasUpi = partner.getUpiId() != null && !partner.getUpiId().isBlank();
        boolean hasBankTrio = partner.getBankAccountHolderName() != null && !partner.getBankAccountHolderName().isBlank()
                && partner.getBankAccountNumber() != null && !partner.getBankAccountNumber().isBlank()
                && partner.getBankIfscCode() != null && !partner.getBankIfscCode().isBlank();
        if (!hasUpi && !hasBankTrio) {
            missing.add("Bank details");
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

    @Override
    @Transactional
    public PartnerProfileResponse updateDutyStatus(UUID partnerId, DutyStatusRequest request) {
        DeliveryPartner partner = findPartner(partnerId);
        DutyStatus requested = request.getStatus();

        // FR-3.1/FR-3.4 — the partner may only ever request these two states
        // directly; ON_DELIVERY is reserved for the (future) assignment flow.
        if (requested != DutyStatus.ONLINE && requested != DutyStatus.OFFLINE) {
            throw new IllegalArgumentException("Status must be ONLINE or OFFLINE.");
        }
        if (requested == DutyStatus.ONLINE && partner.getLifecycleState() != PartnerLifecycleState.APPROVED) {
            throw new IllegalArgumentException("Only approved partners can go online.");
        }
        if (requested == DutyStatus.OFFLINE && partner.getDutyStatus() == DutyStatus.ON_DELIVERY) {
            throw new IllegalArgumentException("You can't go offline while on a delivery.");
        }

        partner.setDutyStatus(requested);
        partner.setUpdatedAt(LocalDateTime.now());
        partnerRepository.save(partner);

        return PartnerProfileResponse.from(partner, listDocuments(partnerId));
    }

    @Override
    @Transactional
    public void updateLocation(UUID partnerId, double latitude, double longitude) {
        DeliveryPartner partner = findPartner(partnerId);
        partner.setCurrentLatitude(latitude);
        partner.setCurrentLongitude(longitude);
        partner.setLastLocationAt(LocalDateTime.now());
        partnerRepository.save(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(UUID partnerId) {
        DeliveryPartner partner = findPartner(partnerId);

        // FR-3.8/M4-M6 — activeDelivery reflects a real in-flight assignment.
        DashboardSummaryResponse.ActiveDeliveryResponse activeDelivery = assignmentRepository
                .findFirstByPartnerIdAndStatusInOrderByOfferedAtDesc(partnerId, DASHBOARD_ACTIVE_STATUSES)
                .map(DashboardSummaryResponse.ActiveDeliveryResponse::from)
                .orElse(null);

        // FR-3.6/M6 — today's real DELIVERED assignments, not a placeholder 0.
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<DeliveryAssignment> deliveredToday = assignmentRepository.findByPartnerIdAndStatusAndDeliveredAtBetween(
                partnerId, AssignmentStatus.DELIVERED, startOfToday, startOfToday.plusDays(1));
        long todayEarningsPaise = deliveredToday.stream().mapToLong(DeliveryAssignment::getEstimatedPayoutPaise).sum();

        return DashboardSummaryResponse.builder()
                .dutyStatus(partner.getDutyStatus())
                .todayDeliveries(deliveredToday.size())
                .todayEarningsPaise(todayEarningsPaise)
                .rating(partner.getRating())
                .lowRatingWarning(partner.getRating() != null && partner.getRating() < lowRatingThreshold)
                .activeDelivery(activeDelivery)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CashInHandResponse getCashInHand(UUID partnerId) {
        DeliveryPartner partner = findPartner(partnerId);
        return toCashInHandResponse(partner);
    }

    @Override
    @Transactional
    public CashInHandResponse remitCash(UUID partnerId, RemittanceMethod method) {
        DeliveryPartner partner = findPartner(partnerId);

        // FR-7.3 — mirrors the client-side gate that only shows "Remit to
        // ThaliCloud" once the threshold is crossed.
        if (partner.getCashInHandPaise() < remittanceThresholdPaise) {
            throw new IllegalArgumentException("Cash in Hand must reach the remittance threshold before you can remit.");
        }

        // FR-7.4 — Phase 1: no live UPI payout/support-point integration and no
        // Ops console anywhere in this workspace, so the balance is debited on
        // submission (same "immediate effect + recorded trail for later
        // reconciliation" spirit as requestCancellation in
        // DeliveryAssignmentServiceImpl) rather than held pending approval.
        CashRemittance remittance = new CashRemittance();
        remittance.setPartner(partner);
        remittance.setAmountPaise(partner.getCashInHandPaise());
        remittance.setMethod(method);
        remittanceRepository.save(remittance);

        partner.setCashInHandPaise(0L);
        partnerRepository.save(partner);

        return toCashInHandResponse(partner);
    }

    private CashInHandResponse toCashInHandResponse(DeliveryPartner partner) {
        return CashInHandResponse.builder()
                .cashInHandPaise(partner.getCashInHandPaise())
                .remittanceThresholdPaise(remittanceThresholdPaise)
                .canRemit(partner.getCashInHandPaise() >= remittanceThresholdPaise)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EarningsSummaryResponse getEarningsSummary(UUID partnerId, EarningsPeriod period) {
        findPartner(partnerId); // 404s if the partner doesn't exist, mirroring every other method here.

        LocalDate today = LocalDate.now();
        LocalDate periodStart;
        LocalDate periodEndExclusive; // the day *after* the last day included, for a half-open DB range query.
        switch (period) {
            case WEEK -> {
                periodStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                periodEndExclusive = periodStart.plusDays(7);
            }
            case MONTH -> {
                periodStart = today.withDayOfMonth(1);
                periodEndExclusive = periodStart.plusMonths(1);
            }
            default -> {
                periodStart = today;
                periodEndExclusive = today.plusDays(1);
            }
        }

        List<DeliveryAssignment> delivered = assignmentRepository.findByPartnerIdAndStatusAndDeliveredAtBetween(
                partnerId, AssignmentStatus.DELIVERED, periodStart.atStartOfDay(), periodEndExclusive.atStartOfDay());

        long totalEarningsPaise = delivered.stream().mapToLong(DeliveryAssignment::getEstimatedPayoutPaise).sum();
        int deliveryCount = delivered.size();
        long averagePayoutPaise = deliveryCount == 0 ? 0 : totalEarningsPaise / deliveryCount;

        List<EarningsSummaryResponse.DeliveryEarningResponse> deliveries = delivered.stream()
                .sorted(Comparator.comparing(DeliveryAssignment::getDeliveredAt).reversed())
                .map(EarningsSummaryResponse.DeliveryEarningResponse::from)
                .toList();

        return EarningsSummaryResponse.builder()
                .period(period.name())
                .periodStart(periodStart)
                .periodEnd(periodEndExclusive.minusDays(1))
                .totalEarningsPaise(totalEarningsPaise)
                .deliveryCount(deliveryCount)
                .averagePayoutPaise(averagePayoutPaise)
                .deliveries(deliveries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutResponse> getPayoutHistory(UUID partnerId) {
        return payoutRepository.findByPartnerIdOrderByInitiatedAtDesc(partnerId).stream()
                .map(PayoutResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PayoutResponse createPayout(UUID partnerId, long amountPaise, String destinationReference, LocalDate periodStart, LocalDate periodEnd) {
        DeliveryPartner partner = findPartner(partnerId);

        Payout payout = new Payout();
        payout.setPartner(partner);
        payout.setAmountPaise(amountPaise);
        payout.setDestinationReference(destinationReference);
        payout.setPeriodStart(periodStart);
        payout.setPeriodEnd(periodEnd);
        payout.setStatus(PayoutStatus.PROCESSING);
        payoutRepository.save(payout);

        return PayoutResponse.from(payout);
    }

    @Override
    @Transactional
    public PayoutResponse updatePayoutStatus(UUID payoutId, PayoutStatus status) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));

        payout.setStatus(status);
        payout.setPaidAt(status == PayoutStatus.PAID ? LocalDateTime.now() : null);
        payoutRepository.save(payout);

        // M12/FR-12.1
        if (status == PayoutStatus.PAID) {
            notificationService.notify(payout.getPartner().getId(), NotificationType.PAYOUT_PROCESSED,
                    "Payout processed",
                    "Your payout of ₹%d has been paid out.".formatted(payout.getAmountPaise() / 100),
                    payout.getId().toString());
        }

        return PayoutResponse.from(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeliveryHistoryItemResponse> getDeliveryHistory(UUID partnerId, int page, int size) {
        findPartner(partnerId); // 404s if the partner doesn't exist, mirroring every other method here.

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "offeredAt"));
        Page<DeliveryAssignment> results = assignmentRepository
                .findByPartnerIdAndStatusInOrderByOfferedAtDesc(partnerId, HISTORY_STATUSES, pageRequest);

        return PageResponse.from(results.map(DeliveryHistoryItemResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryHistoryDetailResponse getDeliveryHistoryDetail(UUID partnerId, UUID assignmentId) {
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        // Same "not found" (rather than "forbidden") response as findOwnedAssignment
        // in DeliveryAssignmentServiceImpl avoids leaking existence of another
        // partner's assignment; likewise for a still-in-flight one (not history yet).
        if (!assignment.getPartner().getId().equals(partnerId) || !HISTORY_STATUSES.contains(assignment.getStatus())) {
            throw new ResourceNotFoundException("Delivery not found");
        }
        return DeliveryHistoryDetailResponse.from(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryFeedbackResponse> getRecentFeedback(UUID partnerId) {
        findPartner(partnerId); // 404s if the partner doesn't exist, mirroring every other method here.
        return assignmentRepository.findFirst20ByPartnerIdAndCustomerRatingIsNotNullOrderByRatedAtDesc(partnerId).stream()
                .map(DeliveryFeedbackResponse::from)
                .toList();
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
