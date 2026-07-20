package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.request.CreateAssignmentRequest;
import com.thalicloud.delivery.dto.response.AssignmentResponse;
import com.thalicloud.delivery.enums.CancelledBy;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentService {

    // FR-4.1/FR-4.6 — rejects if the partner already has an active offer/assignment,
    // or isn't ONLINE. Pushes the offer to /topic/partner/{partnerId}/request.
    AssignmentResponse createAssignment(CreateAssignmentRequest request);

    // Resume support — lets the app recover an in-flight OFFERED/ACCEPTED
    // assignment after a restart.
    Optional<AssignmentResponse> getCurrentAssignment(UUID partnerId);

    // FR-4.4 — transitions the partner to ON_DELIVERY.
    AssignmentResponse accept(UUID partnerId, UUID assignmentId);

    // FR-4.4/FR-4.5
    AssignmentResponse decline(UUID partnerId, UUID assignmentId);

    // FR-4.8 — releases the partner back to ONLINE and records the cancellation.
    AssignmentResponse requestCancellation(UUID partnerId, UUID assignmentId, String reason);

    // FR-4.7 — internal/order-service-facing. Pushes an alert to
    // /topic/partner/{partnerId}/cancellation and releases the partner.
    AssignmentResponse systemCancel(UUID assignmentId, CancelledBy cancelledBy);

    // FR-4.3/FR-4.5 — sweeps OFFERED assignments whose countdown ran out.
    void expireStaleOffers();
}
