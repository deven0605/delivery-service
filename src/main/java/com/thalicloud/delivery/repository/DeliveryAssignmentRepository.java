package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    // FR-4.6 — used to enforce "one active assignment at a time".
    Optional<DeliveryAssignment> findFirstByPartnerIdAndStatusInOrderByOfferedAtDesc(
            UUID partnerId, List<AssignmentStatus> statuses);

    // FR-4.3/FR-4.5 — the scheduled expiry sweep.
    List<DeliveryAssignment> findByStatusAndExpiresAtBefore(AssignmentStatus status, LocalDateTime cutoff);

    // M6/FR-3.6 — today's completed deliveries, for the dashboard's earnings summary.
    List<DeliveryAssignment> findByPartnerIdAndStatusAndDeliveredAtBetween(
            UUID partnerId, AssignmentStatus status, LocalDateTime start, LocalDateTime end);

    // M9 — Delivery History (FR-9.1/FR-9.3). offeredAt ordering mirrors the
    // "one active assignment at a time" invariant (FR-4.6): a partner never
    // has two deliveries in flight, so offer order == completion order.
    Page<DeliveryAssignment> findByPartnerIdAndStatusInOrderByOfferedAtDesc(
            UUID partnerId, List<AssignmentStatus> statuses, Pageable pageable);

    // M10 — Ratings & Feedback (FR-10.1). Recomputed fresh after every new
    // rating rather than tracked incrementally — simplest correct rolling
    // average given the low per-partner delivery volumes here.
    @Query("select avg(a.customerRating) from DeliveryAssignment a where a.partner.id = :partnerId and a.customerRating is not null")
    Double findAverageRatingByPartnerId(@Param("partnerId") UUID partnerId);

    // M10 — Ratings & Feedback (FR-10.2). Most recent rated deliveries first.
    List<DeliveryAssignment> findFirst20ByPartnerIdAndCustomerRatingIsNotNullOrderByRatedAtDesc(UUID partnerId);
}
