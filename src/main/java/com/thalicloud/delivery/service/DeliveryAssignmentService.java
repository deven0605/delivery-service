package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.request.CreateAssignmentRequest;
import com.thalicloud.delivery.dto.request.DispatchOrderRequest;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.dto.response.DispatchOrderResponse;
import com.thalicloud.delivery.enums.CancelledBy;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentService {

    // FR-4.1/FR-4.6 — rejects if the partner already has an active offer/assignment,
    // or isn't ONLINE. Pushes the offer to /topic/partner/{partnerId}/request.
    AssignmentResponse createAssignment(CreateAssignmentRequest request);

    // Order-service-facing (see InternalAssignmentController) — picks the first
    // ONLINE partner with no active assignment and offers them the delivery.
    // Returns assigned=false (not an error) if no partner is currently available.
    DispatchOrderResponse dispatchOrder(DispatchOrderRequest request);

    // Resume support — lets the app recover an in-flight OFFERED/ACCEPTED
    // assignment after a restart.
    Optional<AssignmentResponse> getCurrentAssignment(UUID partnerId);

    // FR-4.4 — transitions the partner to ON_DELIVERY.
    AssignmentResponse accept(UUID partnerId, UUID assignmentId);

    // FR-4.4/FR-4.5
    AssignmentResponse decline(UUID partnerId, UUID assignmentId);

    // FR-5.4 — records arrival at the kitchen; location is best-effort (may be null).
    AssignmentResponse arriveAtKitchen(UUID partnerId, UUID assignmentId, Double latitude, Double longitude);

    // FR-5.5/FR-5.6 — verifies the 4-digit pickup code (typed or scanned from a QR
    // code) and transitions the assignment to PICKED_UP.
    AssignmentResponse verifyPickup(UUID partnerId, UUID assignmentId, String code);

    // FR-6.1 — PICKED_UP -> OUT_FOR_DELIVERY, marking the start of the drop leg.
    AssignmentResponse startDropNavigation(UUID partnerId, UUID assignmentId);

    // FR-6.3 — records arrival at the drop location; location is best-effort (may be null).
    AssignmentResponse arriveAtDrop(UUID partnerId, UUID assignmentId, Double latitude, Double longitude);

    // FR-6.5 — uploads a Contactless Drop proof-of-delivery photo; doesn't change status.
    AssignmentResponse uploadDropPhoto(UUID partnerId, UUID assignmentId, String fileName, String contentType, byte[] data);

    // FR-7.2 — marks the COD amount as collected and credits it to the
    // partner's Cash in Hand balance; verifyDelivery requires this first for
    // COD assignments (FR-7.1).
    AssignmentResponse markCashCollected(UUID partnerId, UUID assignmentId);

    // FR-6.4/FR-6.5/FR-6.6/FR-6.7 — verifies the delivery OTP (or, if contactless,
    // requires an already-uploaded proof photo), transitions to DELIVERED, and
    // releases the partner back to ONLINE.
    AssignmentResponse verifyDelivery(UUID partnerId, UUID assignmentId, String otp, boolean contactless);

    // FR-4.8 — releases the partner back to ONLINE and records the cancellation.
    AssignmentResponse requestCancellation(UUID partnerId, UUID assignmentId, String reason);

    // FR-4.7 — internal/order-service-facing. Pushes an alert to
    // /topic/partner/{partnerId}/cancellation and releases the partner.
    AssignmentResponse systemCancel(UUID assignmentId, CancelledBy cancelledBy);

    // FR-4.3/FR-4.5 — sweeps OFFERED assignments whose countdown ran out.
    void expireStaleOffers();

    // M10/FR-10.1/FR-10.2 — internal/order-service-facing (see
    // InternalAssignmentController). Only a DELIVERED assignment may be rated;
    // recomputes the partner's rolling average rating afterward.
    void rateDelivery(UUID assignmentId, int rating, String feedback);
}
