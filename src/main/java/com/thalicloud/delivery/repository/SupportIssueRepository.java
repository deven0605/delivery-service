package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.SupportIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportIssueRepository extends JpaRepository<SupportIssue, UUID> {

    // FR-13.2 — the partner's own reported issues, most recent first.
    List<SupportIssue> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId);
}
