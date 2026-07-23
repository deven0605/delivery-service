package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.client.OrderStatusCallbackClient;
import com.thalicloud.delivery.dto.request.CreateAssignmentRequest;
import com.thalicloud.delivery.dto.request.DispatchOrderRequest;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.dto.response.DispatchOrderResponse;
import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.enums.AssignmentStatus;
import com.thalicloud.delivery.enums.CancelledBy;
import com.thalicloud.delivery.enums.DutyStatus;
import com.thalicloud.delivery.enums.NotificationType;
import com.thalicloud.delivery.enums.PaymentMethod;
import com.thalicloud.delivery.exception.ResourceNotFoundException;
import com.thalicloud.delivery.repository.DeliveryAssignmentRepository;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import com.thalicloud.delivery.service.DocumentStorageService;
import com.thalicloud.delivery.service.NotificationService;
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

    // M5/M6 extend "active" through the whole pickup+drop flow so
    // /assignments/current keeps resolving (resume support) and a new offer
    // can't be created mid-delivery. DELIVERED is deliberately excluded —
    // it's terminal, and the partner is free for a new offer immediately.
    private static final List<AssignmentStatus> ACTIVE_STATUSES = List.of(
            AssignmentStatus.OFFERED, AssignmentStatus.ACCEPTED,
            AssignmentStatus.ARRIVED_AT_KITCHEN, AssignmentStatus.PICKED_UP,
            AssignmentStatus.OUT_FOR_DELIVERY, AssignmentStatus.ARRIVED_AT_DROP);

    // FR-5.4/FR-6.3 — informational only (logged, not enforced): the client
    // already gates the "Arrived at..." buttons via geofence-or-delay fallback.
    private static final double ARRIVAL_GEOFENCE_METERS = 200.0;

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentStorageService documentStorageService;
    private final NotificationService notificationService;
    private final OrderStatusCallbackClient orderStatusCallbackClient;

    @Value("${assignment.offer-ttl-seconds}")
    private long offerTtlSeconds;

    // FR-10.3 — same configurable advisory threshold DeliveryPartnerServiceImpl
    // uses for the dashboard banner; also drives the M12 RATING_ALERT notification.
    @Value("${rating.low-threshold}")
    private double lowRatingThreshold;

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
        // FR-7.1 — codAmountPaise is only meaningful (and required) for COD orders;
        // not expressible as declarative bean validation since it's conditional.
        if (request.getPaymentMethod() == PaymentMethod.COD && request.getCodAmountPaise() == null) {
            throw new IllegalArgumentException("codAmountPaise is required when paymentMethod is COD.");
        }
        // FR-8.2 — the earnings breakdown must add up to the total payout.
        long breakdownSum = request.getBaseFarePaise() + request.getDistanceFarePaise() + request.getIncentivePaise();
        if (breakdownSum != request.getEstimatedPayoutPaise()) {
            throw new IllegalArgumentException("baseFarePaise + distanceFarePaise + incentivePaise must equal estimatedPayoutPaise.");
        }

        LocalDateTime now = LocalDateTime.now();
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setPartner(partner);
        assignment.setOrderId(request.getOrderId());
        assignment.setKitchenName(request.getKitchenName());
        assignment.setKitchenDistanceKm(request.getKitchenDistanceKm());
        assignment.setDropLocality(request.getDropLocality());
        assignment.setEstimatedPayoutPaise(request.getEstimatedPayoutPaise());
        assignment.setBaseFarePaise(request.getBaseFarePaise());
        assignment.setDistanceFarePaise(request.getDistanceFarePaise());
        assignment.setIncentivePaise(request.getIncentivePaise());
        assignment.setEstimatedDistanceKm(request.getEstimatedDistanceKm());
        assignment.setItemCount(request.getItemCount());
        assignment.setKitchenLatitude(request.getKitchenLatitude());
        assignment.setKitchenLongitude(request.getKitchenLongitude());
        assignment.setKitchenContactNumber(request.getKitchenContactNumber());
        assignment.setDropLatitude(request.getDropLatitude());
        assignment.setDropLongitude(request.getDropLongitude());
        assignment.setCustomerContactNumber(request.getCustomerContactNumber());
        assignment.setPaymentMethod(request.getPaymentMethod());
        assignment.setCodAmountPaise(request.getPaymentMethod() == PaymentMethod.COD ? request.getCodAmountPaise() : null);
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
        // M12/FR-12.1 — in-app record of the same event; stands in for the FCM
        // backgrounded-fallback wake-up until real push infra exists.
        notificationService.notify(partner.getId(), NotificationType.NEW_ASSIGNMENT,
                "New delivery request", "%s → %s".formatted(assignment.getKitchenName(), assignment.getDropLocality()),
                assignment.getId().toString());
        return response;
    }

    @Override
    @Transactional
    public DispatchOrderResponse dispatchOrder(DispatchOrderRequest request) {
        // No geo/zone matching engine exists yet (see CreateAssignmentRequest's
        // own docs) — pick the first ONLINE partner with no active assignment.
        Optional<DeliveryPartner> candidate = partnerRepository.findByDutyStatus(DutyStatus.ONLINE).stream()
                .filter(p -> assignmentRepository
                        .findFirstByPartnerIdAndStatusInOrderByOfferedAtDesc(p.getId(), ACTIVE_STATUSES)
                        .isEmpty())
                .findFirst();

        if (candidate.isEmpty()) {
            return new DispatchOrderResponse(false, null, null, "No delivery partner available right now.");
        }

        DeliveryPartner partner = candidate.get();
        LocalDateTime now = LocalDateTime.now();

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setPartner(partner);
        assignment.setOrderId(request.getOrderId());
        assignment.setKitchenName(request.getKitchenName());
        // Phase 1 placeholders — order-service doesn't have kitchen geo-coordinates
        // or a real fare/distance engine (see DeliveryDispatchClient's own docs).
        assignment.setKitchenDistanceKm(0.0);
        assignment.setKitchenLatitude(0.0);
        assignment.setKitchenLongitude(0.0);
        assignment.setKitchenContactNumber(request.getKitchenContactNumber());
        assignment.setDropLocality(request.getDropAddress());
        assignment.setDropLatitude(request.getDropLatitude());
        assignment.setDropLongitude(request.getDropLongitude());
        assignment.setCustomerContactNumber(request.getCustomerContactNumber());
        assignment.setEstimatedDistanceKm(0.0);
        assignment.setItemCount(request.getItemCount());
        assignment.setEstimatedPayoutPaise(request.getDeliveryChargePaise());
        assignment.setBaseFarePaise(request.getDeliveryChargePaise());
        assignment.setDistanceFarePaise(0L);
        assignment.setIncentivePaise(0L);
        assignment.setPaymentMethod(request.getPaymentMethod());
        assignment.setCodAmountPaise(request.getPaymentMethod() == PaymentMethod.COD ? request.getCodAmountPaise() : null);
        assignment.setStatus(AssignmentStatus.OFFERED);
        assignment.setOfferedAt(now);
        assignment.setExpiresAt(now.plusSeconds(offerTtlSeconds));
        assignmentRepository.save(assignment);

        partner.setTotalAssignments(partner.getTotalAssignments() + 1);
        partnerRepository.save(partner);

        AssignmentResponse response = AssignmentResponse.from(assignment);
        messagingTemplate.convertAndSend("/topic/partner/" + partner.getId() + "/request", response);
        notificationService.notify(partner.getId(), NotificationType.NEW_ASSIGNMENT,
                "New delivery request", "%s → %s".formatted(assignment.getKitchenName(), assignment.getDropLocality()),
                assignment.getId().toString());

        return new DispatchOrderResponse(true, assignment.getId(), partner.getId(), "Assignment offered");
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

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setRespondedAt(now);
        assignment.setAcceptedAt(now);
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
    public AssignmentResponse arriveAtKitchen(UUID partnerId, UUID assignmentId, Double latitude, Double longitude) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() == AssignmentStatus.ARRIVED_AT_KITCHEN) {
            // Idempotent — a duplicate tap (slow network retry) is a no-op, not an error.
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new IllegalArgumentException("This delivery isn't awaiting pickup.");
        }

        if (latitude != null && longitude != null) {
            double distanceMeters = haversineMeters(latitude, longitude, assignment.getKitchenLatitude(), assignment.getKitchenLongitude());
            log.debug("Partner {} marked arrival {}m from kitchen (geofence {}m) for assignment {}",
                    partnerId, Math.round(distanceMeters), (long) ARRIVAL_GEOFENCE_METERS, assignmentId);
        }

        assignment.setStatus(AssignmentStatus.ARRIVED_AT_KITCHEN);
        assignment.setArrivedAtKitchenAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse verifyPickup(UUID partnerId, UUID assignmentId, String code) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() == AssignmentStatus.PICKED_UP) {
            // Idempotent — same reasoning as arriveAtKitchen above.
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getStatus() != AssignmentStatus.ARRIVED_AT_KITCHEN) {
            throw new IllegalArgumentException("Mark yourself as arrived at the kitchen first.");
        }
        if (!assignment.getPickupCode().equals(code)) {
            throw new IllegalArgumentException("Incorrect pickup code. Please check with the kitchen and try again.");
        }

        // FR-5.6
        assignment.setStatus(AssignmentStatus.PICKED_UP);
        assignment.setPickedUpAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // Best-effort: order-service reflects this as DISPATCHED ("Partner
        // Assigned"/"Out for Delivery") — see OrderStatusCallbackClient.
        orderStatusCallbackClient.updateOrderStatus(assignment.getOrderId(), "DISPATCHED");

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse startDropNavigation(UUID partnerId, UUID assignmentId) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() == AssignmentStatus.OUT_FOR_DELIVERY) {
            // Idempotent — DropNavigationScreen calls this on every mount.
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getStatus() != AssignmentStatus.PICKED_UP) {
            throw new IllegalArgumentException("This delivery hasn't been picked up yet.");
        }

        // FR-6.1
        assignment.setStatus(AssignmentStatus.OUT_FOR_DELIVERY);
        assignment.setOutForDeliveryAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse arriveAtDrop(UUID partnerId, UUID assignmentId, Double latitude, Double longitude) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() == AssignmentStatus.ARRIVED_AT_DROP) {
            // Idempotent — same reasoning as arriveAtKitchen above.
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getStatus() != AssignmentStatus.OUT_FOR_DELIVERY) {
            throw new IllegalArgumentException("This delivery isn't out for delivery yet.");
        }

        if (latitude != null && longitude != null) {
            double distanceMeters = haversineMeters(latitude, longitude, assignment.getDropLatitude(), assignment.getDropLongitude());
            log.debug("Partner {} marked drop arrival {}m from drop location (geofence {}m) for assignment {}",
                    partnerId, Math.round(distanceMeters), (long) ARRIVAL_GEOFENCE_METERS, assignmentId);
        }

        assignment.setStatus(AssignmentStatus.ARRIVED_AT_DROP);
        assignment.setArrivedAtDropAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse uploadDropPhoto(UUID partnerId, UUID assignmentId, String fileName, String contentType, byte[] data) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        // FR-6.5 — can be captured any time during the drop leg, ahead of verifyDelivery.
        if (assignment.getStatus() != AssignmentStatus.OUT_FOR_DELIVERY && assignment.getStatus() != AssignmentStatus.ARRIVED_AT_DROP) {
            throw new IllegalArgumentException("You can only capture a proof-of-delivery photo during an active drop.");
        }

        // Reuses the KYC-document bucket (no separate proof-of-delivery bucket exists
        // in this workspace) — the "pod/" prefix keeps object keys from colliding.
        String objectKey = "pod/" + assignmentId + "/" + System.currentTimeMillis() + "-" + fileName;
        String url = documentStorageService.upload(objectKey, data, contentType);

        assignment.setProofOfDeliveryPhotoUrl(url);
        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse markCashCollected(UUID partnerId, UUID assignmentId) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.isCodCollected()) {
            // Idempotent — same reasoning as arriveAtKitchen above.
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getPaymentMethod() != PaymentMethod.COD) {
            throw new IllegalArgumentException("This order isn't Cash on Delivery.");
        }
        if (assignment.getStatus() != AssignmentStatus.ARRIVED_AT_DROP) {
            throw new IllegalArgumentException("Mark yourself as arrived at the drop location first.");
        }

        // FR-7.2
        assignment.setCodCollected(true);
        assignment.setCodCollectedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        DeliveryPartner partner = assignment.getPartner();
        partner.setCashInHandPaise(partner.getCashInHandPaise() + assignment.getCodAmountPaise());
        partnerRepository.save(partner);

        return AssignmentResponse.from(assignment);
    }

    @Override
    @Transactional
    public AssignmentResponse verifyDelivery(UUID partnerId, UUID assignmentId, String otp, boolean contactless) {
        DeliveryAssignment assignment = findOwnedAssignment(partnerId, assignmentId);

        if (assignment.getStatus() == AssignmentStatus.DELIVERED) {
            // Idempotent — same reasoning as verifyPickup above.
            return AssignmentResponse.from(assignment);
        }
        if (assignment.getStatus() != AssignmentStatus.ARRIVED_AT_DROP) {
            throw new IllegalArgumentException("Mark yourself as arrived at the drop location first.");
        }
        // FR-7.1 — the exact COD amount must be collected before OTP/contactless
        // confirmation is allowed; mirrors the client-side gate on this same rule.
        if (assignment.getPaymentMethod() == PaymentMethod.COD && !assignment.isCodCollected()) {
            throw new IllegalArgumentException("Please collect the COD amount before confirming delivery.");
        }

        if (contactless) {
            // FR-6.5 — customer unreachable: a proof photo stands in for the OTP.
            if (assignment.getProofOfDeliveryPhotoUrl() == null) {
                throw new IllegalArgumentException("Please capture a proof-of-delivery photo first.");
            }
            assignment.setContactlessDrop(true);
        } else {
            // FR-6.4
            if (otp == null || otp.isBlank()) {
                throw new IllegalArgumentException("Delivery OTP is required.");
            }
            if (!assignment.getDeliveryOtp().equals(otp)) {
                throw new IllegalArgumentException("Incorrect delivery OTP. Please try again.");
            }
        }

        // FR-6.6
        assignment.setStatus(AssignmentStatus.DELIVERED);
        assignment.setDeliveredAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // FR-6.7
        DeliveryPartner partner = assignment.getPartner();
        partner.setDutyStatus(DutyStatus.ONLINE);
        partnerRepository.save(partner);

        // Best-effort: order-service flips the order to DELIVERED — see
        // OrderStatusCallbackClient.
        orderStatusCallbackClient.updateOrderStatus(assignment.getOrderId(), "DELIVERED");

        return AssignmentResponse.from(assignment);
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusMeters = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
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
        // M12/FR-12.1
        notificationService.notify(partner.getId(), NotificationType.ORDER_CANCELLED,
                "Order cancelled", "%s → %s was cancelled by the %s.".formatted(
                        assignment.getKitchenName(), assignment.getDropLocality(), cancelledBy.name().toLowerCase()),
                assignment.getId().toString());
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

    @Override
    @Transactional
    public void rateDelivery(UUID assignmentId, int rating, String feedback) {
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (assignment.getStatus() != AssignmentStatus.DELIVERED) {
            throw new IllegalArgumentException("Only a completed delivery can be rated.");
        }

        assignment.setCustomerRating(rating);
        assignment.setCustomerFeedback(feedback);
        assignment.setRatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // FR-10.1 — recomputed fresh rather than tracked incrementally; see
        // DeliveryAssignmentRepository#findAverageRatingByPartnerId.
        DeliveryPartner partner = assignment.getPartner();
        Double average = assignmentRepository.findAverageRatingByPartnerId(partner.getId());
        if (average != null) {
            double previousRating = partner.getRating() != null ? partner.getRating() : 5.0;
            double newRating = Math.round(average * 10) / 10.0;
            partner.setRating(newRating);
            partnerRepository.save(partner);

            // M12/FR-12.1 — only fires on the transition into low-rating
            // territory, not on every rating below it, so the partner isn't
            // renotified on each subsequent low rating.
            if (newRating < lowRatingThreshold && previousRating >= lowRatingThreshold) {
                notificationService.notify(partner.getId(), NotificationType.RATING_ALERT,
                        "Your rating needs attention",
                        "Your rating has dropped to %.1f, below the platform's advisory threshold.".formatted(newRating),
                        null);
            }
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
