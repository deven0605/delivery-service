package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import com.thalicloud.delivery.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    // FR-4.6 — used to enforce "one active assignment at a time".
    Optional<DeliveryAssignment> findFirstByPartnerIdAndStatusInOrderByOfferedAtDesc(
            UUID partnerId, List<AssignmentStatus> statuses);

    // M3.2 integration — the Home dashboard's "Active Delivery" card.
    Optional<DeliveryAssignment> findFirstByPartnerIdAndStatusOrderByOfferedAtDesc(
            UUID partnerId, AssignmentStatus status);

    // FR-4.3/FR-4.5 — the scheduled expiry sweep.
    List<DeliveryAssignment> findByStatusAndExpiresAtBefore(AssignmentStatus status, LocalDateTime cutoff);
}
