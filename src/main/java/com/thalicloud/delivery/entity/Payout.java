package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// M8.2 — Payout History (FR-8.4/FR-8.5). Owned outright by delivery-service
// (new table, same pattern as DeliveryAssignment/CashRemittance). Phase 1 has
// no Ops batch-payout job anywhere in this workspace, so rows here are only
// ever created via InternalPayoutController's dispatch-key-protected seam —
// same spirit as InternalAssignmentController — rather than by any partner
// action (FR-8.5: "partner cannot request instant payout in Phase 1").
@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    @Column(nullable = false)
    private long amountPaise;

    // A denormalized snapshot of the partner's bank/UPI details at payout time
    // (e.g. "UPI: partner@upi" or "Bank: ****1234"), same reasoning as the
    // rest of this service's denormalized-snapshot fields.
    @Column(nullable = false, length = 100)
    private String destinationReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PayoutStatus status = PayoutStatus.PROCESSING;

    // FR-8.5 — the weekly cycle this payout covers.
    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PayoutStatus.PROCESSING;
        }
    }
}
