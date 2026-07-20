package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.dto.request.CreateAssignmentRequest;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.enums.AssignmentStatus;
import com.thalicloud.delivery.enums.CancelledBy;
import com.thalicloud.delivery.enums.DutyStatus;
import com.thalicloud.delivery.exception.ResourceNotFoundException;
import com.thalicloud.delivery.repository.DeliveryAssignmentRepository;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {

    private static final List<AssignmentStatus> ACTIVE_STATUSES = List.of(AssignmentStatus.OFFERED, AssignmentStatus.ACCEPTED);

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${assignment.offer-ttl-seconds}")
    private long offerTtlSeconds;

    @Override
    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        DeliveryPartner partner = partnerRepository.findById(request.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

        // FR-3.1 (implicit) / FR-4.6 — only an idle, online partner can be offered a new request.
        if (partner.getDutyStatus() != DutyStatus.ONLINE) {
            throw new IllegalArgumentException("Partner is not online.");
        }
        if (assignmentRepository.findFirstByPartnerIdAndStatusInOrderByOfferedAtDesc(partner.getId(), ACTIVE_STATUSES).isPresent()) {
            throw new IllegalArgumentException("Partner already has an active assignment.");
        }

        LocalDateTime now = LocalDateTime.now();
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setPartner(partner);
        assignment.setOrderId(request.getOrderId());
        assignment.setKitchenName(request.getKitchenName());
        assignment.setKitchenDistanceKm(request.getKitchenDistanceKm());
        assignment.setDropLocality(request.getDropLocality());
        assignment.setEstimatedPayoutPaise(request.getEstimatedPayoutPaise());
        assignment.setEstimatedDistanceKm(request.getEstimatedDistanceKm());
        assignment.setItemCount(request.getItemCount());
        assignment.setStatus(AssignmentStatus.OFFERED);
        assignment.setOfferedAt(now);
        assignment.setExpiresAt(now.plusSeconds(offerTtlSeconds));
        assignmentRepository.save(assignment);

        partner.setTotalAssignments(partner.getTotalAssignments() + 1);
        partnerRepository.save(partner);

        AssignmentResponse response = AssignmentResponse.from(assignment);
        // FR-4.1 — push over STOMP; FCM wake-up fallback isn't implemented (no
        // Firebase/Redis infra exists anywhere in the workspace to build on).
        messagingTemplate.convertAndSend("/topic/partner/" + partner.getId() + "/request", response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentResponse> getCurrentAssignment(UUID partnerId) {
        return assignmentRepository.findFirstByPartnerIdAndStatusInOrderByOfferedAtDesc(partnerId, ACTIVE_STATUSES)
                .map(AssignmentResponse::from);
    }

    @Override
    @Transactional
    public AssignmentResponse accept(UUID partnerId, UUID assignmentId) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() != AssignmentStatus.OFFERED) {
            throw new IllegalArgumentException("This request is no longer available.");
        }
        if (LocalDateTime.now().isAfter(assignment.getExpiresAt())) {
            assignment.setStatus(AssignmentStatus.EXPIRED);
            assignment.setRespondedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);
            throw new IllegalArgumentException("This request has expired.");
        }

        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setRespondedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // FR-4.4
        DeliveryPartner partner = assignment.getPartner();
        partner.setDutyStatus(DutyStatus.ON_DELIVERY);
        partnerRepository.save(partner);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse decline(UUID partnerId, UUID assignmentId) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        // Idempotent — a decline racing the expiry sweep (or a repeat tap) is a no-op, not an error.
        if (assignment.getStatus() == AssignmentStatus.DECLINED || assignment.getStatus() == AssignmentStatus.EXPIRED) {
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getStatus() != AssignmentStatus.OFFERED) {
            throw new IllegalArgumentException("This request has already been accepted.");
        }

        assignment.setStatus(AssignmentStatus.DECLINED);
        assignment.setRespondedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // FR-4.5 — reassigning to the next-nearest available partner is dispatch/matching
        // logic that doesn't exist anywhere in the workspace yet; out of scope here.
        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse requestCancellation(UUID partnerId, UUID assignmentId, String reason) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only an accepted delivery can be cancelled.");
        }

        assignment.setStatus(AssignmentStatus.CANCELLED_BY_PARTNER);
        assignment.setCancellationReason(reason);
        assignment.setRespondedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // FR-4.8 — releases the partner immediately; the reason is retained for
        // Ops to review afterward. There's no admin/ops console in this
        // workspace, so "requires Ops/system confirmation" is represented only
        // as the recorded reason + cancelledAssignments counter, not a
        // blocking approval step.
        DeliveryPartner partner = assignment.getPartner();
        partner.setDutyStatus(DutyStatus.ONLINE);
        partner.setCancelledAssignments(partner.getCancelledAssignments() + 1);
        partnerRepository.save(partner);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse systemCancel(UUID assignmentId, CancelledBy cancelledBy) {
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new IllegalArgumentException("Assignment is not active.");
        }

        assignment.setStatus(cancelledBy == CancelledBy.CUSTOMER
                ? AssignmentStatus.CANCELLED_BY_CUSTOMER
                : AssignmentStatus.CANCELLED_BY_KITCHEN);
        assignment.setRespondedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        DeliveryPartner partner = assignment.getPartner();
        partner.setDutyStatus(DutyStatus.ONLINE);
        partnerRepository.save(partner);

        AssignmentResponse response = AssignmentResponse.from(assignment);
        // FR-4.7 — "immediate in-app alert".
        messagingTemplate.convertAndSend("/topic/partner/" + partner.getId() + "/cancellation", response);
        return response;
    }

    @Override
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void expireStaleOffers() {
        List<DeliveryAssignment> stale = assignmentRepository.findByStatusAndExpiresAtBefore(AssignmentStatus.OFFERED, LocalDateTime.now());
        for (DeliveryAssignment assignment : stale) {
            assignment.setStatus(AssignmentStatus.EXPIRED);
            assignment.setRespondedAt(LocalDateTime.now());
        }
        if (!stale.isEmpty()) {
            assignmentRepository.saveAll(stale);
            log.debug("Expired {} stale assignment offer(s)", stale.size());
        }
    }

    private DeliveryAssignment findOwnedAssignment(UUID partnerId, UUID assignmentId) {
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (!assignment.getPartner().getId().equals(partnerId)) {
            throw new ResourceNotFoundException("Assignment not found");
        }
        return assignment;
    }
}
