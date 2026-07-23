package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.PartnerNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnerNotificationRepository extends JpaRepository<PartnerNotification, UUID> {

    // FR-12.3 — most recent first, paginated.
    Page<PartnerNotification> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId, Pageable pageable);

    // FR-12.2 — drives the bell's unread badge count.
    long countByPartnerIdAndReadFalse(UUID partnerId);

    List<PartnerNotification> findByPartnerIdAndReadFalse(UUID partnerId);
}
