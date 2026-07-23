package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.RemittanceMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

// M7/FR-7.3-FR-7.4 — an audit trail entry for a Cash-in-Hand remittance.
// Owned outright by delivery-service (new table, no auth-service mirror
// needed, same pattern as DeliveryAssignment/KYCDocument). Phase 1 has no
// Ops console anywhere in this workspace, so — same spirit as
// DeliveryAssignmentServiceImpl's requestCancellation — the partner's
// cashInHandPaise is debited immediately on submission; this row is the
// only record left for Ops to reconcile against later, not a blocking
// approval gate.
@Entity
@Table(name = "cash_remittances")
@Getter
@Setter
@NoArgsConstructor
public class CashRemittance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    @Column(nullable = false)
    private long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RemittanceMethod method;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
