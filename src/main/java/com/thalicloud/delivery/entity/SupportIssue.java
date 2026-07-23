package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.IssueCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

// M13 — Help & Support (FR-13.2). Owned outright by delivery-service, same as
// DeliveryAssignment. No status/resolution workflow — there's no Ops console
// anywhere in this workspace to action these (same gap as everywhere else
// marked "internal"/"Phase 1"); this is just the submitted-report record.
@Entity
@Table(name = "support_issues")
@Getter
@Setter
@NoArgsConstructor
public class SupportIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    // FR-13.2 — "tied to a specific delivery (optional)". A plain id rather
    // than a @ManyToOne: the report must survive even if fetching the
    // assignment later fails for any reason, and nothing here ever needs to
    // join back to it.
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueCategory category;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
