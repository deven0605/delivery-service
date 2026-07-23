package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    // FR-8.4 — Payout History, most recent first.
    List<Payout> findByPartnerIdOrderByInitiatedAtDesc(UUID partnerId);
}
